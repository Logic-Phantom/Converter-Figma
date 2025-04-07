var CPR_DEFAULTS = {
	controls: {
		accordion: {
			childCombinatorClass: "accordion-content",
			clipContent: false,
			titleCollapse: true,
			multiple: true
		},
		audio: {},
		button: {},
		calendar: {
			headerButtons: ["title", "prev", "next"],
			footerVisible: false
		},
		checkbox: {},
		checkboxgroup: {
			colCount: -1,
			fixedWidth: false,
			horizontalSpacing: 8,
			verticalSpacing: 4
		},
		combobox: {
			preventInput: true
		},
		linkedcombobox: {
			space: "4px"
		},
		dateinput: {
			headerButtons: ["title", "prev", "next"],
			footerVisible: false,
			format: "YYYYMMDD",
			mask: "YYYY-MM-DD",
			headerFormat: "YYYY년 MMMM"
		},
		dialog: {},
		embeddedapp: {},
		embeddedpage: {},
		fileinput: {
			showClearButton: true
		},
		fileupload: {
			emptyMessage: "더블 클릭 하거나, 업로드할 파일을 이곳에 끌어 넣으세요.",
			statusMessageExp: "sstr(totalFileCount) + ' 개, ' + sstr(totalFileSize + ' ' + totalFileSizeUnit)"
		},
		grid: {
			collapsible: true,
			columnMovable: true,
			noDataMessage: "표시할 데이터가 없습니다.",
			clickMode: "edit",
			viewingMode: "button",
			filterDialogButtons: ["cancel", "okay"],
			layout: {
				controls: {
					button: {
						topSpacing: 0,
						rightSpacing: 0,
						bottomSpacing: 0,
						leftSpacing: 0
					},
					checkbox: {},
					checkboxgroup: {},
					combobox: {},
					dateinput: {},
					fileinput: {},
					htmlsnippet: {},
					image: {},
					maskeditor: {},
					numbereditor: {},
					inputbox: {},					
					output: {},
					progress: {
						verticalAlign: "middle",
						height: "16px"
					},
					radiobutton: {},
					searchinput: {},
					slider: {},
					textarea: {},
					treecell: {}
				}
			}
		},
		container: {
			childCombinatorClass: "group-content"
		},
		htmlobject: {},
		htmlsnippet: {},
		image: {},
		inputbox: {
			showClearButton: true
		},
		listbox: {},
		linkedlistbox: {
			space: "4px"
		},
		maskeditor: {
			showClearButton: true
		},
		mdifolder: {
			itemSpacing: -1
		},
		menu: {},
		navigationbar: {
			barScrollButtonPosition: "right"
		},
		numbereditor: {
			spinButton: false,
			showClearButton: true
		},
		notifier: {
			animation: "fadein",
			maxNotifyCount: 1,
			close: true,
			infoClose: true,
			successClose: true,
			warningClose: true,
			dangerClose: true
		},
		output: {
			unselectable: false
		},
		pageindexer: {
			pageIndexWidth: "20px"
		},
		progress: {},
		radiobutton: {
			colCount: -1,
			fixedWidth: false,
			horizontalSpacing: 8,
			verticalSpacing: 4
		},
		searchinput: {},
		sidenavigation: {
			indent: 0,
			selectionHighlightType: "independent"
		},
		slider: {},
		tabfolder: {
			childCombinatorClass: "tabfolder-content",
			headerArrowPosition: "right",
			itemSizing: "auto",
			itemSpacing: -1
		},
		textarea: {},
		tree: {
			indent: 16,
			showLines: true
		},
		treecell: {
			indent: 16,
			showLines: true
		},
		uicontrolshell: {},
		video: {}
	},
	layouts: {
		xylayout: {
			scrollable: false
		},
		responsivexylayout: {
			scrollable: false
		},
		formlayout: {
			horizontalSpacing: "4px",
			verticalSpacing: "4px",
			scrollable: false
		},
		verticallayout: {
			spacing: 4
			//scrollable: false
		},
		flowlayout: {
			horizontalSpacing: 4,
			verticalSpacing: 4,
			scrollable: false
		}
	},
	environment: {
		/* 구동 환경 설정. 로깅, Plugin 관련, 전역이미지, ... */
		/* 활성화 AppInstance가 없을 때 App을 정의하는 JavaScript 객체를 캐시할지 여부를 설정합니다.
		 * false로 설정되면 활성화된 AppInsance가 없을 때 App을 삭제하고 
		 * 다음 App의 요청이 있을 때 App 정의 객체를 서버로 다시 요청합니다.
		 * default는 true입니다.
		 */
		appcache: false,
		/* 다국어 바인딩 시 다국어 키에 매핑된 다국어 메시지가 없을 때 빈값 대신 요청된 다국어 키를 리턴할지를 설정합니다.
		 * true로 설정되면 요청된 다국어 키 값에 배핑된 다국어 메시지가 없을 때 요청된 다국어 키를 리턴합니다.
		 * default는 false 입니다.
		 */
		/*
		useKeyInsteadOfNullI18NMessage: false
		*/
		/*
		 * 컨트롤 및 그룹에서 브라우저 내장 스크롤바 대신 스타일 적용이 가능한 커스텀 스크롤바 적용 여부를 설정합니다.
		 * 강제로 브라우저 스크롤이 보이도록 스타일 되어있거나 HTMLObject, UIControlShell, HTMLSnippet을 통해 생성되는 스크롤바에 대해서는 지원하지 않습니다.
		 * 실험적인 기능이라 true로 설정했을 때 기존 스타일에 문제가 있을 수 있습니다.
		 * default는 false 입니다.
		 */
		/*
		useCustomScrollbar: false
		*/
		/**
		 * 생성되는 UIControl에 대응되는 HTMLElement의 ID 속성을 UIControl의 uuid를 사용할 지 여부를 설정합니다.
		 * true로 설정되면 UIControl에 대응되는 HTMLElement의 ID 속성이 UIControl의 uuid를 사용하여 매크로를 통한 위협을 방어할 수 있습니다.
		 * false로 설정되면 UIControl에 대응되는 HTMLElement의 ID 속성이 UIContorl의 경로로 생성되어 
		 * Web UI 자동화 테스트툴을 사용하여 테스트할 때 테스트툴이 테스트 시 동일한 HTMLElement를 찾을 수 있도록 지원합니다.
		 * default는 true 입니다.
		 */
		/*
		useControlUUIDasHTMLId: true
		*/
	}
}