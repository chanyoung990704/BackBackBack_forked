package com.aivle.project.company.batch;

import com.aivle.project.company.config.DartProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

/**
 * DART 기업 코드 Reader.
 */
@Component
@RequiredArgsConstructor
public class DartCorpCodeItemReader implements ItemStreamReader<DartCorpCodeItem> {

private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private final DartProperties dartProperties;
	private final WebClient dartWebClient;

	private ZipInputStream zipInputStream;
	private InputStream xmlInputStream;
	private XMLEventReader eventReader;
	private boolean statusChecked;

	@Override
	public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
		String apiKey = dartProperties.getApiKey();
		if (!StringUtils.hasText(apiKey)) {
			throw new ItemStreamException("DART_API_KEY가 설정되지 않았습니다.");
		}
		try {
			DartProperties.Http http = dartProperties.getHttp();
			byte[] zipBytes = dartWebClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/api/corpCode.xml")
					.queryParam("crtfc_key", apiKey)
					.build())
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.retrieve()
				.bodyToMono(byte[].class)
				.retryWhen(Retry.backoff(http.getRetryCount(), Duration.ofMillis(http.getRetryBackoffMs())))
				.block();
			if (zipBytes == null || zipBytes.length == 0) {
				throw new ItemStreamException("DART 기업 코드 ZIP 응답이 비어 있습니다.");
			}
			zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8);
			ZipEntry entry = nextXmlEntry(zipInputStream);
			if (entry == null) {
				throw new ItemStreamException("DART 기업 코드 ZIP에서 XML 파일을 찾지 못했습니다.");
			}
			xmlInputStream = zipInputStream;
			eventReader = XMLInputFactory.newFactory().createXMLEventReader(xmlInputStream, StandardCharsets.UTF_8.name());
			statusChecked = false;
		} catch (IOException | XMLStreamException ex) {
			throw new ItemStreamException("DART 기업 코드 ZIP 파싱에 실패했습니다.", ex);
		}
	}

	@Override
	public DartCorpCodeItem read() throws Exception {
		if (eventReader == null) {
			return null;
		}
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (!event.isStartElement()) {
				continue;
			}
			String localName = event.asStartElement().getName().getLocalPart();
			if ("status".equals(localName) && !statusChecked) {
				String status = readElementText();
				statusChecked = true;
				if (!"000".equals(status)) {
					throw new ItemStreamException("DART 응답 오류: " + status);
				}
				continue;
			}
			if ("list".equals(localName)) {
				return readListElement();
			}
		}
		return null;
	}

	@Override
	public void close() throws ItemStreamException {
		try {
			if (eventReader != null) {
				eventReader.close();
			}
			if (xmlInputStream != null) {
				xmlInputStream.close();
			}
			if (zipInputStream != null) {
				zipInputStream.close();
			}
		} catch (IOException | XMLStreamException ex) {
			throw new ItemStreamException("DART 기업 코드 리더 종료 중 오류가 발생했습니다.", ex);
		}
	}

	@Override
	public void update(@NonNull ExecutionContext executionContext) {
		// 현재는 체크포인트 저장이 필요하지 않습니다.
	}

	private DartCorpCodeItem readListElement() throws XMLStreamException {
		String corpCode = null;
		String corpName = null;
		String corpEngName = null;
		String stockCode = null;
		LocalDate modifyDate = null;

		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isEndElement() && "list".equals(event.asEndElement().getName().getLocalPart())) {
				break;
			}
			if (!event.isStartElement()) {
				continue;
			}
			String field = event.asStartElement().getName().getLocalPart();
			String value = readElementText();
			if ("corp_code".equals(field)) {
				corpCode = value;
			} else if ("corp_name".equals(field)) {
				corpName = value;
			} else if ("corp_eng_name".equals(field)) {
				corpEngName = value;
			} else if ("stock_code".equals(field)) {
				stockCode = value;
			} else if ("modify_date".equals(field)) {
				modifyDate = parseDate(value);
			}
		}
		return new DartCorpCodeItem(corpCode, corpName, corpEngName, stockCode, modifyDate);
	}

	private String readElementText() throws XMLStreamException {
		String text = eventReader.getElementText();
		return text == null ? null : text.trim();
	}

	private LocalDate parseDate(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return LocalDate.parse(value.trim(), DATE_FORMATTER);
	}

	private ZipEntry nextXmlEntry(ZipInputStream zipStream) throws IOException {
		ZipEntry entry;
		while ((entry = zipStream.getNextEntry()) != null) {
			if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
				return entry;
			}
		}
		return null;
	}
}
