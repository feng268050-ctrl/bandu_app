import 'package:bandu_wrong_notebook/application/app/app_router.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_models.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/domain/question_bank_repository.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/exam_management_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/pdf_question_bank_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/presentation/question_set_management_page.dart';
import 'package:bandu_wrong_notebook/application/features/question_bank/question_bank_providers.dart';
import 'package:bandu_wrong_notebook/components/design_system/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

void main() {
  testWidgets('PDF import actions stay fixed while the question list scrolls',
      (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(
            _FakeQuestionBankRepository(),
          ),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.dark),
          home: const PdfQuestionBankPage(),
        ),
      ),
    );
    await tester.pump();
    await tester.tap(find.text('选择 PDF 文件'));
    await tester.pumpAndSettle();

    final confirm = find.byKey(const Key('confirm-pdf-import'));
    final cancel = find.byKey(const Key('cancel-pdf-import'));
    expect(confirm, findsOneWidget);
    expect(cancel, findsOneWidget);
    expect(find.text('确认导入 60 道题'), findsOneWidget);
    expect(tester.getBottomRight(cancel).dy, lessThanOrEqualTo(640));

    final initialTop = tester.getTopLeft(confirm).dy;
    await tester.drag(find.byType(CustomScrollView), const Offset(0, -2000));
    await tester.pumpAndSettle();

    expect(tester.getTopLeft(confirm).dy, closeTo(initialTop, 0.01));
    expect(tester.takeException(), isNull);
  });

  testWidgets('exam dialog only lists detected types and updates total count',
      (tester) async {
    tester.view.physicalSize = const Size(430, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(
            _FakeQuestionBankRepository(
              questionSets: [_mixedQuestionSet()],
            ),
          ),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.dark),
          home: const PdfQuestionBankPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('生成随机考卷'));
    await tester.pumpAndSettle();

    expect(find.text('单选题'), findsOneWidget);
    expect(find.text('多选题'), findsOneWidget);
    expect(find.text('填空题'), findsNothing);
    expect(find.text('大题'), findsNothing);
    expect(find.text('共 5 题'), findsOneWidget);

    final singleCountField =
        find.byKey(const Key('exam-count-value-singleChoice'));
    await tester.tap(singleCountField);
    await tester.enterText(singleCountField, '1');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pump();

    expect(find.text('共 4 题'), findsOneWidget);
    expect(
      tester.widget<TextField>(singleCountField).controller?.text,
      '1',
    );

    final decrementSingle =
        find.byKey(const Key('exam-count-decrement-singleChoice'));
    await tester.tap(decrementSingle);
    await tester.pump();

    expect(find.text('共 3 题'), findsOneWidget);
    expect(
      tester.widget<TextField>(singleCountField).controller?.text,
      '0',
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('saved exam remains visible after leaving the exam page',
      (tester) async {
    final questionSet = _mixedQuestionSet();
    final session = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {
        BankQuestionType.singleChoice: 1,
        BankQuestionType.multipleChoice: 2,
      },
      sessionId: 'saved-exam',
      seed: 42,
      createdAt: DateTime.utc(2026, 7, 18, 14, 30),
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(
            _FakeQuestionBankRepository(
              questionSets: [questionSet],
              examSessions: [session],
            ),
          ),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.dark),
          home: const PdfQuestionBankPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('已生成考卷'), findsOneWidget);
    expect(find.byKey(const Key('manage-exams')), findsOneWidget);
    expect(find.text('选择题题集 随机考卷'), findsOneWidget);
    expect(find.textContaining('继续作答 · 已提交 0 / 3 题'), findsOneWidget);
    expect(
      find.textContaining('单选题 1 道 · 多选题 2 道 · 共 3 题'),
      findsOneWidget,
    );
  });

  testWidgets('exam management renames and deletes a saved exam',
      (tester) async {
    final questionSet = _mixedQuestionSet();
    final session = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {BankQuestionType.singleChoice: 1},
      sessionId: 'managed-exam',
      seed: 42,
      createdAt: DateTime.utc(2026, 7, 18, 14, 30),
    );
    final repository = _FakeQuestionBankRepository(
      questionSets: [questionSet],
      examSessions: [session],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.dark),
          home: const ExamManagementPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('exam-menu-managed-exam')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('重命名'));
    await tester.pumpAndSettle();
    final titleInput = find.byKey(const Key('exam-title-input'));
    await tester.enterText(titleInput, '期中练习卷');
    await tester.tap(find.byKey(const Key('confirm-exam-rename')));
    await tester.pumpAndSettle();

    expect(find.text('期中练习卷'), findsOneWidget);
    expect(repository.examSessions.single.title, '期中练习卷');

    await tester.tap(find.byKey(const ValueKey('exam-menu-managed-exam')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('删除'));
    await tester.pumpAndSettle();
    expect(find.text('删除考卷'), findsOneWidget);
    await tester.tap(find.text('删除').last);
    await tester.pumpAndSettle();

    expect(find.text('暂无已生成考卷'), findsOneWidget);
    expect(repository.examSessions, isEmpty);
  });

  testWidgets('saved exam management entry resolves through the app route',
      (tester) async {
    final questionSet = _mixedQuestionSet();
    final session = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {BankQuestionType.singleChoice: 1},
      sessionId: 'route-exam',
      seed: 42,
      createdAt: DateTime.utc(2026, 7, 18, 14, 30),
    );
    final router = GoRouter(
      initialLocation: '/capture/pdf-import',
      routes: [
        GoRoute(
          path: '/capture',
          builder: (context, state) => const SizedBox.shrink(),
          routes: buildQuestionBankRoutes(),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(
            _FakeQuestionBankRepository(
              questionSets: [questionSet],
              examSessions: [session],
            ),
          ),
        ],
        child: MaterialApp.router(
          theme: buildAppTheme(Brightness.dark),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('manage-exams')));
    await tester.pumpAndSettle();

    expect(router.state.uri.path, '/capture/pdf-import/exams');
    expect(find.text('管理考卷'), findsOneWidget);
    expect(find.text('Page Not Found'), findsNothing);
  });

  testWidgets('question set management renames and deletes an imported PDF',
      (tester) async {
    final questionSet = _mixedQuestionSet();
    final session = createRandomExamSession(
      questionSet: questionSet,
      questionCounts: const {BankQuestionType.singleChoice: 1},
      sessionId: 'linked-exam',
      seed: 42,
      createdAt: DateTime.utc(2026, 7, 18, 14, 30),
    );
    final repository = _FakeQuestionBankRepository(
      questionSets: [questionSet],
      examSessions: [session],
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(repository),
        ],
        child: MaterialApp(
          theme: buildAppTheme(Brightness.dark),
          home: const QuestionSetManagementPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const ValueKey('question-set-menu-mixed-set')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('重命名'));
    await tester.pumpAndSettle();
    final nameInput = find.byKey(const Key('question-set-name-input'));
    await tester.enterText(nameInput, 'ACP 326 题');
    await tester.tap(find.byKey(const Key('confirm-question-set-rename')));
    await tester.pumpAndSettle();

    expect(find.text('ACP 326 题'), findsOneWidget);
    expect(repository.questionSets.single.name, 'ACP 326 题');

    await tester.tap(find.byKey(const ValueKey('question-set-menu-mixed-set')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('删除'));
    await tester.pumpAndSettle();
    expect(find.text('删除题集'), findsOneWidget);
    await tester.tap(find.text('删除').last);
    await tester.pumpAndSettle();

    expect(find.text('暂无已导入 PDF'), findsOneWidget);
    expect(repository.questionSets, isEmpty);
    expect(repository.examSessions, isEmpty);
  });

  testWidgets('imported PDF management entry resolves through the app route',
      (tester) async {
    final questionSet = _mixedQuestionSet();
    final router = GoRouter(
      initialLocation: '/capture/pdf-import',
      routes: [
        GoRoute(
          path: '/capture',
          builder: (context, state) => const SizedBox.shrink(),
          routes: buildQuestionBankRoutes(),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          questionBankRepositoryProvider.overrideWithValue(
            _FakeQuestionBankRepository(questionSets: [questionSet]),
          ),
        ],
        child: MaterialApp.router(
          theme: buildAppTheme(Brightness.dark),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('manage-question-sets')));
    await tester.pumpAndSettle();

    expect(router.state.uri.path, '/capture/pdf-import/banks');
    expect(find.text('管理题集'), findsOneWidget);
    expect(find.text('Page Not Found'), findsNothing);
  });
}

class _FakeQuestionBankRepository implements QuestionBankRepository {
  _FakeQuestionBankRepository({
    List<PdfQuestionSet> questionSets = const [],
    List<ExamSession> examSessions = const [],
  })  : questionSets = [...questionSets],
        examSessions = [...examSessions];

  final List<PdfQuestionSet> questionSets;
  final List<ExamSession> examSessions;

  @override
  Future<String?> pickPdf() async => '/tmp/questions.pdf';

  @override
  Future<PdfImportPreview> analyzePdf(
    String localPdfPath, {
    String? modelId,
  }) async {
    return PdfImportPreview(
      fileName: 'questions.pdf',
      localPdfPath: localPdfPath,
      totalPages: 12,
      parser: 'test',
      questions: [
        for (var index = 0; index < 60; index++)
          BankQuestion(
            id: 'q-$index',
            stem: '第 ${index + 1} 题',
            options: const ['A. 1', 'B. 2'],
            answer: 'B',
            questionType: BankQuestionType.singleChoice,
            difficulty: BankQuestionDifficulty.medium,
            tags: const [],
            sourcePage: index ~/ 5 + 1,
            needsReview: true,
          ),
      ],
    );
  }

  @override
  Future<List<PdfQuestionSet>> loadQuestionSets() async => questionSets;

  @override
  Future<PdfQuestionSet> saveQuestionSet(PdfImportPreview preview) {
    throw UnimplementedError();
  }

  @override
  Future<PdfQuestionSet> renameQuestionSet(
    String questionSetId,
    String name,
  ) async {
    final index = questionSets.indexWhere((set) => set.id == questionSetId);
    if (index < 0) throw StateError('question_bank_not_found');
    final updated = questionSets[index].copyWith(name: name.trim());
    questionSets[index] = updated;
    return updated;
  }

  @override
  Future<void> deleteQuestionSet(String questionSetId) async {
    final before = questionSets.length;
    questionSets.removeWhere((set) => set.id == questionSetId);
    if (questionSets.length == before) {
      throw StateError('question_bank_not_found');
    }
    examSessions
        .removeWhere((session) => session.questionSetId == questionSetId);
  }

  @override
  Future<ExamSession> createExam(
    String questionSetId,
    Map<BankQuestionType, int> questionCounts,
  ) {
    throw UnimplementedError();
  }

  @override
  Future<List<ExamSession>> loadExamSessions() async => examSessions;

  @override
  Future<ExamSession> renameExamSession(String sessionId, String title) async {
    final index = examSessions.indexWhere((session) => session.id == sessionId);
    if (index < 0) throw StateError('exam_session_not_found');
    final updated = examSessions[index].copyWith(title: title.trim());
    examSessions[index] = updated;
    return updated;
  }

  @override
  Future<void> deleteExamSession(String sessionId) async {
    examSessions.removeWhere((session) => session.id == sessionId);
  }

  @override
  Future<ExamSession?> loadExamSession(String sessionId) {
    throw UnimplementedError();
  }

  @override
  Future<ExamSession> submitExamAnswer(
    String sessionId,
    String attemptId,
    String userAnswer,
  ) {
    throw UnimplementedError();
  }
}

PdfQuestionSet _mixedQuestionSet() {
  return PdfQuestionSet(
    id: 'mixed-set',
    name: '选择题题集',
    sourceFileName: 'choices.pdf',
    localPdfPath: '/tmp/choices.pdf',
    importedAt: DateTime.utc(2026, 7, 18),
    questions: [
      for (var index = 0; index < 2; index++)
        BankQuestion(
          id: 'single-$index',
          stem: '单选题 $index',
          options: const ['A. 1', 'B. 2'],
          answer: 'A',
          questionType: BankQuestionType.singleChoice,
          difficulty: BankQuestionDifficulty.easy,
          tags: const [],
          needsReview: false,
        ),
      for (var index = 0; index < 3; index++)
        BankQuestion(
          id: 'multiple-$index',
          stem: '多选题 $index',
          options: const ['A. 1', 'B. 2'],
          answer: 'A,B',
          questionType: BankQuestionType.multipleChoice,
          difficulty: BankQuestionDifficulty.medium,
          tags: const [],
          needsReview: false,
        ),
    ],
  );
}
