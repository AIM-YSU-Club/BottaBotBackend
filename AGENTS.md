# Project agent instructions

## API documentation

- REST API를 추가하거나 변경할 때 Springdoc OpenAPI/Swagger 어노테이션을 함께 작성한다.
- 컨트롤러에는 기능 영역을 설명하는 `@Tag`를 작성한다.
- 각 API 메서드에는 `@Operation`으로 summary와 description을 작성한다.
- 예상 가능한 성공 및 오류 응답은 `@ApiResponses`와 `@ApiResponse`로 문서화한다.
- 요청 DTO, 응답 DTO 및 주요 필드에는 `@Schema`로 설명과 적절한 예시를 작성한다.
- Path/Query/Header 파라미터에는 필요한 경우 `@Parameter`를 작성한다.
- Swagger 설명은 실제 HTTP method, path, status code 및 응답 구조와 일치해야 한다.

## Java documentation

- 새로 추가하거나 변경하는 클래스, 인터페이스, 메서드 및 주요 DTO에는 기능을 설명하는 Javadoc을 작성한다.
- 기능 설명 주석은 `//` 또는 일반 블록 주석보다 `/** ... */` Javadoc 형식을 사용한다.
- public 메서드의 Javadoc에는 필요한 경우 `@param`, `@return`, `@throws`를 작성한다.
- 주석은 코드가 무엇을 하는지 단순 반복하지 말고 책임, 정책, 예외 조건 및 보안상 이유를 설명한다.
- 구현이 변경되면 관련 Javadoc과 Swagger 문서도 함께 갱신한다.

## Commit and pull request types

- 커밋과 PR에는 아래 타입만 사용한다.
  - `Feat`: 새로운 기능 추가
  - `Fix`: 버그 수정
  - `Refactor`: 동작 변경 없는 코드 구조 개선
  - `Docs`: 문서만 변경
  - `Style`: 포맷팅, 세미콜론, 들여쓰기 등 로직에 영향 없는 변경
  - `Test`: 테스트 코드 추가 또는 수정
  - `Chore`: 빌드 설정, 패키지 매니저, 환경 설정 등 잡무성 변경
  - `Perf`: 성능 개선
  - `Build`: 빌드 시스템 또는 외부 의존성 변경
  - `CI`: CI/CD 설정 변경
  - `Revert`: 이전 커밋 되돌리기
- 커밋은 변경 타입별로 논리적인 단위를 나눠 작성한다.
- 커밋 제목 형식은 `<Type>: <작업 내용 요약>`을 사용한다.
- PR 제목 형식은 `[<Type>] <작업 내용 요약>`을 사용한다.

## Pull request template

PR 본문은 아래 형식을 사용한다.

```markdown
## 작업 내용
- 무엇을 했는지 간단히 (불릿 2~4개)

## 변경 이유 / 배경
- 왜 이 작업이 필요했는지 (이슈 링크 연결)

## 변경 사항
- 구체적으로 어떤 파일/로직이 바뀌었는지
- 스크린샷이나 GIF (UI 변경 시 필수)

## 테스트 방법
- 리뷰어가 어떻게 확인하면 되는지 (실행 명령어, 테스트 시나리오)

## 체크리스트
- [ ] 내용
- [ ] 내용
- [ ] 내용

Closes #이슈번호
관련 이슈: #이슈번호, #이슈번호

리뷰어: @팀원닉네임
```

- 연결할 이슈나 지정 리뷰어가 없으면 해당 바닥글 항목은 추측해서 작성하지 않는다.
- UI 변경이 없으면 스크린샷/GIF 항목에 해당 없음으로 명시한다.
