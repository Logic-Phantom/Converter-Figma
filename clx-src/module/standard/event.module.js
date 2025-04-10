/************************************************
 * event.module.js
 * Created at 2021. 10. 15 오후 4:56:54.
 *
 * @author
 ************************************************/

(function () {
	/*
	 * 앱 init시 처리로직 담당 변수
	 * - 반응형 작업
	 */
	if(AppProperties.RUN_APP_INIT_TASK === true) {
		var vaCtl = null;	// 대상컨트롤
		
		/* 반응형 모듈 적용 처리	 */
		function fnResponsiveMdl (e) {
			if (!(e.control instanceof cpr.core.AppInstance)) return;
			
			/** @type cpr.core.AppInstance */
			var appInstance = e.control;
			vaCtl.some(function(each) {
				
				if (each instanceof cpr.controls.Container) {
					/* 폼 레이아웃 반응형 처리*/
					if (each.getLayout() instanceof cpr.controls.layouts.FormLayout && (each.userAttr("mobile-column-count") != "" || each.userAttr("tablet-column-count") != "")) {
						var rForm = makeResponsive(each);
						
						each["_RForm"] = rForm;
						var supportedMediaName = each.getAppInstance().allSupportedMediaNames;
						
						// responsiveForm - mobile col settings
						var vaMobileScrnName = rForm._screenNms.mobile.filter(function(scrn) {
							return (supportedMediaName.indexOf(scrn) != -1)
						})
						if (vaMobileScrnName.length > 0) rForm.setColumnSettings(vaMobileScrnName[0], parseInt(each.userAttr(rForm.ATTR_NM.ATTR_MOBILE_COLUMN_COUNT) || "0"));
						
						// responsiveForm - tablet col settings
						var vaTabletScrnName = rForm._screenNms.tablet.filter(function(scrn) {
							return (supportedMediaName.indexOf(scrn) != -1)
						})
						if (vaTabletScrnName.length > 0) rForm.setColumnSettings(vaTabletScrnName[0], parseInt(each.userAttr(rForm.ATTR_NM.ATTR_TABLET_COLUMN_COUNT) || "0"));
						
						rForm.start();

					/* 버티컬 레이아웃 반응형 처리*/
					} else if (each.getLayout() instanceof cpr.controls.layouts.VerticalLayout && (each.userAttr("mobile-fit") || each.userAttr("tablet-fit"))) {
						var rVertical = makeVResponsive(each);
						each["_RVertical"] = rVertical;
						rVertical.start();
					}
				}
				
				/* 그리드 반응형 처리 */
				if (each instanceof cpr.controls.Grid && each.userAttr("transform-on-mobile") != "") {
					var rGrid = makeResponsiveGrid(each);
					each["_RGrid"] = rGrid;
	
				/* 탭폴더 반응형 처리 */
				} else if (each instanceof cpr.controls.TabFolder && each.userAttr("transform-on-mobile") != "") {
					var rTab = makeResponsiveTab(each);
					each["_RTab"] = rTab;
				}
			});
			
			/*
			 * 루트 레이아웃이 버티컬레이아웃인 경우, 작업영역(grpData) 이 화면 높이 가득 차도록 설정한다.
			 * IS_APP_FILL_SIZE 이 true 인 경우에만 동작한다.
			 */
			if (!ValueUtil.isNull(AppProperties.IS_APP_FILL_SIZE) && AppProperties.IS_APP_FILL_SIZE === true) {
				
				// 메인에 mdiCn(MDI폴더) 가 있을 경우, selection-change 시에 내부 컨텐츠 fillLayout 하도록 설정
				if (appInstance.isRootAppInstance()) {
					var vcMdiCn = appInstance.lookup(AppProperties.MAIN_EMB_CONTROL_ID);
					if (vcMdiCn && (vcMdiCn instanceof cpr.controls.TabFolder)) {
						vcMdiCn.addEventListener("selection-change", function(e) {
							cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function() {
								var content = e.newSelection.content;
								if (content instanceof cpr.controls.EmbeddedApp) {
									var voSelection = content.getEmbeddedAppInstance();
									if (voSelection) {
										var voSelectionRVertical = voSelection.getContainer()["_RVertical"];
										if (voSelectionRVertical) voSelectionRVertical._fillLayout();
									}
								}
							});
						});
					}
				}
				
				// appContainer 가 버티컬레이아웃일 경우, RVertical 이 없는 경우에만 반응형 버티컬 기능 수행
				// mobile-fit, tablet-fit 이 없는 appContainer 를 대상으로 fillLayout 설정하기 위함
				var appContainer = appInstance.getContainer();
				if (appContainer.getLayout() instanceof cpr.controls.layouts.VerticalLayout) {
					var rVerticalLayout = appContainer["_RVertical"];
					if (ValueUtil.isNull(rVerticalLayout)) {
						rVerticalLayout = makeVResponsive(appContainer);
						appContainer["_RVertical"] = rVerticalLayout;
						rVerticalLayout.start();
					}
				}
			}
		}
		
		cpr.events.EventBus.INSTANCE.addFilter("init", function(e) {
			if (e.control instanceof cpr.core.AppInstance) {
				/** @type cpr.core.AppInstance */
				var _app = e.control;
				vaCtl = _app.getContainer().getAllRecursiveChildren(true);
				
				/* 커스텀 옵션 : 반응형 모듈 적용*/
				fnResponsiveMdl(e);
			}
		});
	}
	
	/*
	 * 인풋박스 공통 이벤트처리 담당 변수
	 */
	if(AppProperties.RUN_INPUT_INIT_TASK === true) {
		cpr.events.EventBus.INSTANCE.addFilter("before-value-change", function(e) {
			// 이벤트를 발생 시킨 컨트롤
			/** @type cpr.controls.InputBox */
			var control = e.control;
			
			// 이벤트 발송자가 인풋박스이면.
			if (control.type === "inputbox") {
				var inputLetter = control.userAttr(AppProperties.INPUT_INIT_TASK_ATTR);
				if (inputLetter == "uppercase") {
					if (/[a-z]/g.test(e.newValue)) {
						var newValue = e.newValue.toUpperCase();
						control.value = newValue;
						e.preventDefault();
						e.stopPropagation();
					}
				} else if (inputLetter == "lowercase") {
					if (/[A-Z]/g.test(e.newValue)) {
						var newValue = e.newValue.toLowerCase();
						control.value = newValue;
						e.preventDefault();
						e.stopPropagation();
					}
				}
			}
		});
	}
	
	/*
	 * 그리드 기능별 ui처리 담당변수
	 */
	if(AppProperties.RUN_GRID_INIT_TASK === true) {
		// 모든 selection-change 이벤트시 그리드에 대한  필터 추가(for. 그리드의 선택된 로우가 없을 경우 이벤트 전파 차단)
		cpr.events.EventBus.INSTANCE.addFilter("selection-change", function(e) {
			// 이벤트를 발생 시킨 컨트롤
			var control = e.control;
			
			// 이벤트 발송자가 그리드 이고.
			if (control instanceof cpr.controls.Grid) {
				/** @type cpr.controls.Grid */
				var grid = control;
				if (grid.selectionUnit == "cell" && grid.getSelectedIndices()[0] == null) {
					e.stopPropagation();
				} else {
					var rowIndex = grid.selectionUnit != "cell" ? grid.getSelectedRowIndex() : grid.getSelectedIndices()[0]["rowIndex"];
					// 그리드 선택 ROW가 -1이라면...
					if (rowIndex < 0) {
						// 이벤트 전파를 차단시킵니다.
						e.stopPropagation();
					}
				}
			}
		});
		
		// 모든 before-selection-change 이벤트에시 그리드에 대한  필터만 추가.(for. 그리드의 선택된 로우가 없을 경우 이벤트 전파 차단)
		cpr.events.EventBus.INSTANCE.addFilter("before-selection-change", function(e) {
			// 이벤트를 발생 시킨 컨트롤
			var control = e.control;
			
			// 이벤트 발송자가 그리드 이고.
			if (control instanceof cpr.controls.Grid) {
				// 테스트 화면의 경우 이벤트 적용 안함
				if (e.newSelection[0] == null || e.newSelection[0] == undefined) {
					// 이벤트 전파를 차단시킵니다.
					e.stopPropagation();
				}
			}
		});
	}
	
	/*
	 * Tab Key를 사용하여 tabIndex를 포커스로 명확히 표시하는 기능 
	 */
	if(AppProperties.RUN_KEY_FOCUS_EVENT_TASK === true) {
		cpr.events.EventBus.INSTANCE.addFilter("keydown", function( /* cpr.events.CKeyboardEvent*/ e) {
			// 탭 키가 눌리면 키보드 포커스 스타일 시트를 활성화 함.
			if(e.keyCode == cpr.events.KeyCode.TAB) {
				var keyBoardCssLink = document.querySelector("head link[href*=\"focus.keyboard.css\"]");
				if (keyBoardCssLink) {
					keyBoardCssLink.removeAttribute("disabled");
				}
			}
		});
		
		cpr.events.EventBus.INSTANCE.addFilter("mousedown", function(e) {
			// 마우스가 클릭디면 키보드 포커스 스타일 시트를 비활성화 함.
			var keyBoardCssLink = document.querySelector("head link[href*=\"focus.keyboard.css\"]");
			if (keyBoardCssLink) {
				keyBoardCssLink.setAttribute("disabled", true);
			}
		});
	}
	
	/*
	 * 쿼리 프로바이더 함수 지정
	 */
	if(AppProperties.RUN_QUERY_PROVIDER_TASK === true) {
		/**
		 * 리소스 로더를 통해 로드할 리소스 URL들에 추가 서치 파라미터를 추가할 수 있는 쿼리 프로바이더 함수를 지정 합니다.
		 * 프로바이더 함수는 원본 URL과 캐시 사용 여부를 인자로 받으며 쿼리에 추가될 키/밸류를 담은 JSON 객체를 반환해야 합니다. 
		 * 쿼리 프로바이더는 앱 캐시가 허용되지 않는 경우에 추가로 키들을 공급할 수 있지만, 앱 캐시가 허용된 경우에 제공한 키들을 모두 포함하여야 합니다. 이 조건을 만족하지 않는 쿼리프로바이더가 지정되면 예외가 발생합니다. 
		 * 프로바이더 함수로 null을 지정하면 시스템 기본 동작으로 작동합니다. 
		 * 시스템 기본 동작은 캐시를 사용하지 않도록 지정된 경우 모든 리소스 요청에 대해 `?p=0.3141592` 와 같이 랜덤 숫자를 p 쿼리키로 추가 합니다.
		 * 
		 * 디렉토리 및 파일 단위로 설정시 originURL.pathname으로 조건 체크 예시
		 * if (originURL.pathname.indexOf("app/sample/") > -1  ) {
		 *   
		 */
		cpr.core.ResourceLoader.setQueryProvider(function(originURL, allowsCache) {
			var qryParam = {};
			//defaults.js의 appcache: false 일 경우만 적용
			if (!allowsCache) {
				qryParam = {
					"v": moment().format("YYYYMMDDHHMMSS")
				}
			}
			return qryParam;
		});
	}
})()

