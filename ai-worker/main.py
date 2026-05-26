import os
import sys
import time
import json
import logging
import threading
from typing import Dict, Any
from fastapi import FastAPI
import uvicorn
from confluent_kafka import Consumer, Producer, KafkaError, KafkaException
from fpdf2 import FPDF

# 로깅 설정 (한국어 안내 및 명확한 포맷팅)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(threadName)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("MockAIWorker")

app = FastAPI(
    title="Mock AI Worker Microservice",
    description="카프카 기반 비동기 대용량 처리를 위한 Python Mock AI Worker 마이크로서비스",
    version="1.0.0"
)

# 환경 변수 및 설정 기본값
KAFKA_BOOTSTRAP_SERVERS = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
REQUEST_TOPIC = os.environ.get("KAFKA_REQUEST_TOPIC", "ai-job-request")
RESPONSE_TOPIC = os.environ.get("KAFKA_RESPONSE_TOPIC", "ai-job-response")
CONSUMER_GROUP_ID = os.environ.get("KAFKA_CONSUMER_GROUP", "ai-worker-group")

# Kafka 백그라운드 스레드 제어용 플래그
worker_running = False
worker_thread = None

# PDF 생성을 위한 프리미엄 클래스 설계
class PremiumFinancialReportPDF(FPDF):
    def header(self):
        # 헤더 디자인 (상단 장식선 및 타이틀)
        self.set_fill_color(30, 41, 59)  # Sleek Dark Slate HSL-tailored
        self.rect(0, 0, 210, 15, "F")
        self.set_text_color(255, 255, 255)
        self.set_font("Helvetica", "B", 10)
        self.cell(0, -10, "MOCK AI INSIGHT ENGINE - AUTOMATED FINANCIAL REPORT", align="C")
        self.ln(15)

    def footer(self):
        # 푸터 디자인 (페이지 번호 및 보안 문구)
        self.set_y(-15)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(156, 163, 175)
        self.cell(0, 10, f"Page {self.page_no()} | CONFIDENTIAL - AUTOMATED SYSTEM GENERATED REPORT", align="C")

    def build_report(self, stock_code: str, year: int, quarter: int, predictions: Dict[str, float]):
        self.add_page()
        
        # 문서 타이틀
        self.set_y(25)
        self.set_font("Helvetica", "B", 24)
        self.set_text_color(17, 24, 39)
        self.cell(0, 10, "FINANCIAL ANALYSIS & PROJECTION", ln=True, align="L")
        
        # 메타데이터 박스
        self.set_y(40)
        self.set_fill_color(243, 244, 246)  # Light gray
        self.rect(10, 38, 190, 25, "F")
        
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(75, 85, 99)
        self.cell(40, 8, "Target Company Code: ", ln=False)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(17, 24, 39)
        self.cell(50, 8, f"{stock_code}", ln=False)
        
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(75, 85, 99)
        self.cell(40, 8, "Target Period: ", ln=False)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(17, 24, 39)
        self.cell(50, 8, f"{year} Q{quarter}", ln=True)
        
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(75, 85, 99)
        self.cell(40, 8, "Generated Date: ", ln=False)
        self.set_font("Helvetica", "", 10)
        self.set_text_color(17, 24, 39)
        self.cell(50, 8, f"{time.strftime('%Y-%m-%d %H:%M:%S')}", ln=False)
        
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(75, 85, 99)
        self.cell(40, 8, "Status: ", ln=False)
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(16, 185, 129)  # Beautiful Green
        self.cell(50, 8, "COMPLETED (PROJECTIONS ACTIVE)", ln=True)
        
        self.ln(12)
        
        # 1. AI 예측 지표 섹션 (Table Style)
        self.set_font("Helvetica", "B", 14)
        self.set_text_color(17, 24, 39)
        self.cell(0, 10, "1. Key AI Predictions (Financial Indicators)", ln=True)
        self.ln(2)
        
        # 테이블 헤더
        self.set_fill_color(30, 41, 59)
        self.set_text_color(255, 255, 255)
        self.set_font("Helvetica", "B", 10)
        self.cell(70, 8, "Financial Metric", border=1, align="C", fill=True)
        self.cell(60, 8, "Predicted Value", border=1, align="C", fill=True)
        self.cell(60, 8, "Status / Benchmark", border=1, align="C", fill=True)
        self.ln()
        
        # 테이블 바디
        self.set_text_color(55, 65, 81)
        self.set_font("Helvetica", "", 10)
        
        # 지표별 로우 렌더링
        metrics_meta = {
            "ROA": ("Return on Assets (ROA)", "%", "Stable"),
            "ROE": ("Return on Equity (ROE)", "%", "Healthy Growth"),
            "DEBT_RATIO": ("Debt-to-Equity Ratio", "%", "Moderate Risk")
        }
        
        for code, val in predictions.items():
            meta = metrics_meta.get(code, (code, "", "Calculated"))
            self.cell(70, 8, f" {meta[0]}", border=1)
            self.cell(60, 8, f" {val:.2f}{meta[1]}", border=1, align="C")
            self.cell(60, 8, f" {meta[2]}", border=1, align="C")
            self.ln()
            
        self.ln(10)
        
        # 2. 종합 분석 코멘트 섹션
        self.set_font("Helvetica", "B", 14)
        self.set_text_color(17, 24, 39)
        self.cell(0, 10, "2. Executive AI Comments & Insights", ln=True)
        self.ln(2)
        
        self.set_font("Helvetica", "", 10)
        self.set_text_color(31, 41, 55)
        
        comment_paragraph1 = (
            f"Based on our deep learning-driven financial forecasting engine, the predicted "
            f"financial health of company '{stock_code}' for {year} Q{quarter} exhibits a resilient posture. "
            f"The Return on Assets (ROA) is projected at {predictions.get('ROA', 4.2):.1f}%, indicating robust "
            f"operational efficacy and asset utilization efficiency."
        )
        self.multi_cell(0, 6, comment_paragraph1)
        self.ln(4)
        
        comment_paragraph2 = (
            f"Furthermore, the Return on Equity (ROE) stands at a healthy {predictions.get('ROE', 7.8):.1f}%, "
            f"demonstrating value generation capabilities for shareholders. The Debt Ratio is forecasted "
            f"at a controlled {predictions.get('DEBT_RATIO', 120.5):.1f}%, aligning with conservative risk "
            f"thresholds while sustaining moderate leverage for strategic expansion."
        )
        self.multi_cell(0, 6, comment_paragraph2)
        self.ln(10)
        
        # 3. 면책 조항 및 워닝
        self.set_fill_color(254, 243, 199)  # Warm Gold/Amber light highlight
        self.rect(10, self.get_y(), 190, 20, "F")
        self.set_x(12)
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(180, 83, 9)
        self.cell(0, 5, "DISCLAIMER & WARNING", ln=True)
        self.set_x(12)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(120, 53, 4)
        self.multi_cell(186, 4, "This is an automated report generated by the Mock AI Engine. The values projected in this document are based on statistical simulation and do not constitute formal investment advice.")


@app.get("/health")
def health_check():
    """FastAPI 헬스체크 엔드포인트"""
    return {"status": "UP", "kafka_connected": worker_running}


def process_financial_analysis(message: Dict[str, Any], producer: Producer) -> Dict[str, Any]:
    """재무 수치 분석 작업 처리 (3초 지연 시뮬레이션 및 결과 맵 생성)"""
    logger.info(f"[AI_FINANCIAL_ANALYSIS] Processing job: requestId={message.get('requestId')}")
    
    # 3초 지연 시뮬레이션
    time.sleep(3)
    
    # Mock 예측치 구성
    predictions = {
        "ROA": 4.2,
        "ROE": 7.8,
        "DEBT_RATIO": 120.5
    }
    
    # 성공 응답 생성 (Java 백엔드 및 Saga와의 완벽한 동기화)
    response_payload = {
        "success": True,
        "requestId": message.get("requestId"),
        "type": "AI_FINANCIAL_ANALYSIS",
        "predictions": predictions
    }
    
    return response_payload


def process_comment_compilation(message: Dict[str, Any], producer: Producer) -> Dict[str, Any]:
    """종합 코멘트 컴파일 작업 처리 (3초 지연, fpdf2 기반 동적 PDF 생성 및 로컬 저장)"""
    request_id = message.get("requestId")
    company_id = message.get("companyId")
    year = message.get("year") or 2026
    quarter = message.get("quarter") or 1
    
    # companyId가 6자리 숫자가 아닐 경우, 종목코드로 변환하기 위해 zfill 패딩
    # stockCode 필드가 명시되어 있다면 그것을 우선 사용
    stock_code = message.get("stockCode") or str(company_id).zfill(6)
    
    logger.info(f"[AI_COMMENT_COMPILATION] Processing job: requestId={request_id}, companyId={company_id}, stockCode={stock_code}")
    
    # 3초 지연 시뮬레이션
    time.sleep(3)
    
    # 동적 PDF 저장 디렉토리 및 파일명 계산
    user_home = os.path.expanduser("~")
    reports_dir = os.path.join(user_home, "uploads", "reports", stock_code, str(year), str(quarter))
    os.makedirs(reports_dir, exist_ok=True)
    
    filename = f"report_{stock_code}_{year}_{quarter}.pdf"
    pdf_path = os.path.join(reports_dir, filename)
    
    # fpdf2 라이브러리를 사용하여 프리미엄 PDF 생성
    logger.info(f"Generating dynamic premium PDF at: {pdf_path}")
    pdf = PremiumFinancialReportPDF()
    mock_predictions = {"ROA": 4.2, "ROE": 7.8, "DEBT_RATIO": 120.5}
    pdf.build_report(stock_code, year, quarter, mock_predictions)
    pdf.output(pdf_path)
    
    # 스토리지 키 및 메타데이터 구성 (Java FilesEntity와 필드 호환)
    storage_key = f"reports/{stock_code}/{str(year)}/{str(quarter)}/{filename}"
    
    response_payload = {
        "success": True,
        "requestId": request_id,
        "type": "AI_COMMENT_COMPILATION",
        "storageKey": storage_key,
        "filename": filename
    }
    
    return response_payload


def kafka_consumer_loop():
    """백그라운드에서 Kafka 메시지를 수집하고 작업을 처리하는 컨슈머 루프"""
    global worker_running
    
    logger.info("Initializing Kafka Consumer & Producer...")
    
    # Consumer 설정
    conf_consumer = {
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "group.id": CONSUMER_GROUP_ID,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": True
    }
    
    # Producer 설정 (응답 발행용)
    conf_producer = {
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS
    }
    
    try:
        consumer = Consumer(conf_consumer)
        producer = Producer(conf_producer)
        consumer.subscribe([REQUEST_TOPIC])
        
        logger.info(f"Successfully subscribed to: {REQUEST_TOPIC}")
        worker_running = True
        
        while worker_running:
            # 1.0초 타임아웃으로 메시지 폴링
            msg = consumer.poll(1.0)
            
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # 파티션 끝 도달 (일반적인 이벤트)
                    continue
                else:
                    logger.error(f"Kafka error: {msg.error()}")
                    time.sleep(2)  # 에러 발생 시 일시 지연
                    continue
            
            # 메시지 수집 성공
            payload_str = msg.value().decode("utf-8")
            logger.info(f"Received raw message: {payload_str}")
            
            try:
                # JSON 디코딩 및 검증 (이종 언어 통신 예외 방지)
                message = json.loads(payload_str)
            except json.JSONDecodeError as je:
                logger.error(f"JSON parsing failed for payload. Skipping. Error: {str(je)}")
                continue
            
            job_type = message.get("type")
            request_id = message.get("requestId")
            
            if not job_type or not request_id:
                logger.warning(f"Skipping malformed message (missing type or requestId): {message}")
                continue
            
            response_payload = None
            try:
                # 비즈니스 로직 분기 처리
                if job_type == "AI_FINANCIAL_ANALYSIS":
                    response_payload = process_financial_analysis(message, producer)
                elif job_type == "AI_COMMENT_COMPILATION":
                    response_payload = process_comment_compilation(message, producer)
                else:
                    logger.warning(f"Unsupported job type: {job_type}. Ignoring.")
                    continue
                
            except Exception as ex:
                logger.error(f"Error processing job {job_type} for requestId {request_id}: {str(ex)}", exc_info=True)
                # 실패 응답 구성
                response_payload = {
                    "success": False,
                    "requestId": request_id,
                    "type": job_type,
                    "errorMessage": str(ex)
                }
            
            # 응답 메시지 발행
            if response_payload:
                response_json = json.dumps(response_payload)
                logger.info(f"Publishing response to {RESPONSE_TOPIC}: {response_json}")
                try:
                    producer.produce(
                        RESPONSE_TOPIC,
                        value=response_json.encode("utf-8"),
                        key=request_id.encode("utf-8") if request_id else None
                    )
                    producer.flush()
                    logger.info(f"Successfully published response for requestId: {request_id}")
                except Exception as pe:
                    logger.error(f"Failed to publish response to Kafka: {str(pe)}")
                    
    except Exception as e:
        logger.critical(f"Fatal error in Kafka worker loop: {str(e)}", exc_info=True)
    finally:
        worker_running = False
        try:
            consumer.close()
            logger.info("Kafka Consumer closed safely.")
        except Exception:
            pass


@app.on_event("startup")
def startup_event():
    """서버 시작 시 Kafka Consumer 백그라운드 스레드 시작"""
    global worker_thread, worker_running
    logger.info("FastAPI service starting. Launching background Kafka Consumer thread...")
    worker_running = True
    worker_thread = threading.Thread(
        target=kafka_consumer_loop,
        name="KafkaConsumerThread",
        daemon=True
    )
    worker_thread.start()
    logger.info("Kafka Consumer background thread started.")


@app.on_event("shutdown")
def shutdown_event():
    """서버 종료 시 안전하게 백그라운드 스레드 종료"""
    global worker_running
    logger.info("FastAPI service shutting down. Signaling Kafka thread to stop...")
    worker_running = False
    if worker_thread:
        worker_thread.join(timeout=5.0)
    logger.info("Mock AI Worker cleanly shutdown.")


if __name__ == "__main__":
    # 로컬 개발 및 셋업 검증을 위한 Uvicorn 구동
    uvicorn.run("main:app", host="0.0.0.0", port=8082, reload=False)
