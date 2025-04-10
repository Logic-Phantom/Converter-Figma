/************************************************
 * properties.module.js
 * Created at 2021. 12. 20. 오후 3:36:06.
 *
 * @author 
 ************************************************/
/*
 * ★★ README ★★ 
 * 본 모듈은 프로젝트 내에 필요한 상수를 정의한 모듈입니다.
 * 프로젝트 별 커스터마이징 하여 사용하십시오.
 */

function AppProperties(){};

/* 프로젝트 */
AppProperties.prototype.PROJECT_NM = "eXCFrame-ui" // 프로젝트명

/* 스크린 */
AppProperties.prototype.SCREEN_DEFAULT_NM  = ["default", "EXB-FULL", "EXB-POP[1024]"];	// 기본(pc) 스크린 명
AppProperties.prototype.SCREEN_TABLET_NM   = ["tablet", "EXB-DIV", "EXB-POP[800]"];			// 태블릿 스크린 명
AppProperties.prototype.SCREEN_MOBILE_NM   = ["mobile", "EXB-PART", "EXB-POP[480]"];		// 모바일 스크린 명

/* 메인 */
AppProperties.prototype.MAIN_APP_ID  = "app/com/Main";		// 메인화면 앱 ID
AppProperties.prototype.MSG_TOPIC_ID = "app-msg";				// NotificationCenter 메시지 구독 ID (메인에서 subscribe)
AppProperties.prototype.MAIN_EMB_CONTROL_ID = "mdiCn"; 	// 메인화면 내 콘텐츠(임베디드) 영역 컨트롤 id (MDI폴더 or 임베디드앱)
AppProperties.prototype.MAIN_MENU_INFO = "__menuInfo"; 		// 선택된 메뉴의 메뉴 정보를 담는 사용자속성명, (MDI폴더의 경우, TabItem.userAttr() 로 설정됨)

/* 조회조건 */
AppProperties.prototype.SEARCH_BTN_ID = "btnSearch";	// [조회조건 초기화] 공통 조회 버튼 ID

/* 그리드 */
AppProperties.prototype.GRID_INDEX_COL_HEADER_TEXT = "No";	// [그리드 초기화] 그리드 columnType=rowindex인 컬럼의 헤더 텍스트
AppProperties.prototype.GRID_STATE_COL_HEADER_TEXT = "F";		// [그리드 초기화] 그리드 로우의 CRUD를 표시하는 컬럼의 헤더 텍스트

/* 다이얼로그 */
AppProperties.prototype.DIALOG_MAX_HEIGHT = 760; // 자동높이 사용시(height : -1) 다이얼로그 최대 높이

/* 유효성 체크 */
AppProperties.prototype.VALID_REQUIRED_CLASS = "required" ;	// 필수 style class명  ","(콤마) 구분자로 다중 class 적용.

/* 반응형 */
AppProperties.prototype.IS_APP_FILL_SIZE = true;				 	// 루트레이아웃 버티컬 레이아웃, 작업영역(grpData) 에 대해 화면 높이가 가득 차도록 사이즈 조절 (AppProperties.RUN_APP_INIT_TASK=true 일 경우 적용 가능)
AppProperties.prototype.APP_FILL_SIZE_MIN_HEGIHT = 750; 	// 루트 레이아웃 높이가 750이 넘으면 기존 유지 (AppProperties.prototype.IS_APP_FILL_SIZE=true일 경우 사용)
AppProperties.prototype.FILL_FIX_CTRL_SIZE = true; // 작업영역 사이즈 조절 후, 작업영역 내 그리드, 트리(고정높이)가 가득 차도록 사이즈 조절 (AppProperties.prototype.IS_APP_FILL_SIZE=true일 경우 사용, eXCFrame-ui 에서 제공하는 템플릿 미사용 시 false 처리 권장)

/* event 모듈 동작 */
AppProperties.prototype.RUN_APP_INIT_TASK = true;					// 앱 init 시, 반응형 기능 처리 여부
AppProperties.prototype.RUN_INPUT_INIT_TASK = true; 				// 인풋박스 공통 이벤트 처리 여부 (before-value-change 이벤트에서 uppercase, lowercase 처리)
AppProperties.prototype.RUN_GRID_INIT_TASK = true; 				// 그리드 기능별 ui처리 담당변수 (selection-change, before-selection-change 이벤트 필터 추가)
//AppProperties.prototype.RUN_KEY_FOCUS_EVENT_TASK = false;	// Tab 키를 사용 할 때 포커스 스타일을 표시할지 여부
AppProperties.prototype.RUN_QUERY_PROVIDER_TASK = false;	// 리소스 URL들에 추가 서치 파라미터를 추가할 수 있는 쿼리 프로바이더 함수 지정 여부

AppProperties.prototype.INPUT_INIT_TASK_ATTR = "inputLetter";	// 인풋박스 공통 이벤트 처리를 위한 사용자 속성명 (RUN_INPUT_INIT_TASK=true 일 때 동작)

globals.AppProperties = new AppProperties();
