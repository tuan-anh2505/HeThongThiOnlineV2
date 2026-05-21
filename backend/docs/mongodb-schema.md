# MongoDB Schema - HeThongThiOnlineV2

Tai lieu nay chuan hoa schema MongoDB cho he thong thi online. Cac quan he dung id tham chieu, khong thiet ke join phuc tap. Collection se duoc MongoDB tao khi ung dung insert du lieu dau tien.

## Collections

| Collection | Muc dich | Field chinh | Index chinh |
| --- | --- | --- | --- |
| `accounts` | Tai khoan dang nhap va phan quyen | `username`, `email`, `password`, `fullName`, `dateOfBirth`, `phone`, `role`, `status`, `createdAt`, `updatedAt` | `username` unique, `email` unique, `role`, `status` |
| `students` | Ho so sinh vien | `studentCode`, `accountId`, `mainClassId`, `classIds`, `status` | `studentCode` unique, `accountId` unique, `mainClassId`, `classIds` |
| `teachers` | Ho so giang vien | `teacherCode`, `accountId`, `status`, `classIds`, `subjectIds` | `teacherCode` unique, `accountId` unique, `classIds`, `subjectIds` |
| `admins` | Ho so admin | `adminCode`, `accountId` | `adminCode` unique, `accountId` unique |
| `classes` | Lop hoc/lop hoc phan | `classCode`, `className`, `studentCount`, `teacherId`, `status` | `classCode` unique, `teacherId` |
| `class_students` | Quan he lop - sinh vien | `classId`, `studentId`, `joinedAt`, `status` | compound `classId + studentId`, `classId`, `studentId` |
| `subjects` | Mon thi | `subjectCode`, `subjectName`, `description`, `status` | `subjectCode` unique |
| `class_subject_teachers` | Quan he lop - mon - giang vien | `classId`, `subjectId`, `teacherId`, `semester`, `schoolYear`, `status` | compound `classId + subjectId + teacherId`, `classId`, `subjectId`, `teacherId`, `status` |
| `question_banks` | Ngan hang cau hoi | `name`, `description`, `subjectId`, `teacherId`, `status` | `subjectId`, `teacherId` |
| `questions` | Cau hoi thuoc ngan hang | `questionBankId`, `type`, `content`, `score`, `difficulty`, `topic`, `status`, `answerDefinition` | `questionBankId`, compound `questionBankId + type + difficulty` |
| `exams` | Bai thi | `title`, `classIds`, `subjectId`, `questionBankId`, `teacherId`, `durationMinutes`, `maxAttempts`, `totalScore`, `settings`, `selectionConfig`, `questionRefs`, `status` | `classIds`, `subjectId`, `questionBankId`, `teacherId` |
| `exam_sessions` | Ca thi | `examId`, `startTime`, `endTime`, `status` | `examId` |
| `exam_attempts` | Luot lam bai cua sinh vien | `studentId`, `examId`, `sessionId`, `startedAt`, `submittedAt`, `deadline`, `totalScore`, `status`, `attemptNumber`, `questionSnapshots` | compound unique `examId + studentId + attemptNumber`, `studentId`, `examId`, `sessionId`, `status` |
| `submissions` | Phieu bai lam | `studentId`, `examId`, `sessionId`, `startedAt`, `submittedAt`, `totalScore`, `attemptNumber`, `status` | compound unique `examId + studentId + attemptNumber`, `studentId`, `examId`, `sessionId` |
| `submission_answers` | Chi tiet cau tra loi | `submissionId`, `examId`, `questionId`, `studentAnswer`, `correctAnswerSnapshot`, `score`, `gradingStatus` | compound unique `submissionId + questionId`, `submissionId`, `examId`, `questionId` |
| `exam_results` | Ket qua cong bo/thong ke | `examId`, `classId`, `studentId`, `submissionId`, `score`, `maxScore`, `rank`, `publishStatus` | compound unique `examId + studentId`, `examId`, `classId`, `studentId`, `submissionId` |
| `system_logs` | Nhat ky he thong | `userId`, `action`, `occurredAt`, `ipAddress`, `targetType`, `targetId`, `detail` | compound `userId + occurredAt`, `userId`, `occurredAt`, `targetType`, `targetId` |
| `notifications` | Thong bao | `title`, `content`, `recipientUserId`, `recipientRole`, `recipientGroupId`, `status` | compound `recipientUserId + status`, `recipientUserId`, `recipientRole`, `recipientGroupId` |

## Embedded Documents

| Embedded document | Nam trong collection | Ly do embedded |
| --- | --- | --- |
| `AnswerDefinition` | `questions` | Dap an gan chat voi cau hoi, khong can query doc lap. |
| `AnswerOption` | `questions.answerDefinition.options` | Lua chon trac nghiem co kich thuoc nho. |
| `FillBlankAnswer` | `questions.answerDefinition.fillBlankAnswer` | Cau hinh so khop cua cau hoi dien khuyet. |
| `MatchingPair` | `questions.answerDefinition.matchingPairs` | Cap noi dap an thuoc rieng tung cau hoi. |
| `ExamSettings` | `exams` | Cau hinh hien thi/chon dap an luon di kem bai thi. |
| `QuestionSelectionConfig` | `exams` | Cau hinh tao de khong can collection rieng khi chua co workflow nang cao. |
| `ExamQuestionRef` | `exams.questionRefs` | Danh sach id cau hoi da chon, nho va doc cung bai thi. |

## Tach Rieng Collection

- Tach `students`, `teachers`, `admins` khoi `accounts` de giu tai khoan dang nhap gon va mo rong thong tin rieng theo vai tro.
- Tach `class_students` vi sinh vien co the hoc nhieu lop hoc phan.
- Tach `class_subject_teachers` vi lop, mon va giang vien la quan he N-N.
- Tach `questions` khoi `question_banks` de query, import, random va thong ke cau hoi theo ngan hang.
- Tach `submission_answers` khoi `submissions` de ho tro luu cau tra loi dinh ky va tranh document phieu bai lam qua lon.
- Tach `exam_results` de phuc vu cong bo diem va thong ke nhanh, du co the tai tao tu `submissions`.

## Enum Chinh

- `Role`: `ADMIN`, `TEACHER`, `STUDENT`
- `AccountStatus`: `ACTIVE`, `LOCKED`
- `QuestionType`: `TRUE_FALSE`, `MULTIPLE_CHOICE`, `FILL_BLANK`, `MATCHING`
- `Difficulty`: `EASY`, `MEDIUM`, `HARD`
- `ExamStatus`: `DRAFT`, `PUBLISHED`, `OPEN`, `CLOSED`, `CANCELLED`
- `ExamSessionStatus`: `NOT_OPEN`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`
- `ExamAttemptStatus`: `IN_PROGRESS`, `SUBMITTED`, `EXPIRED`, `CANCELLED`
- `SubmissionStatus`: `IN_PROGRESS`, `SUBMITTED`, `OVERDUE`, `CANCELLED`
- `GradingStatus`: `CORRECT`, `INCORRECT`, `PARTIAL`, `UNGRADED`
- `ResultPublishStatus`: `UNPUBLISHED`, `PUBLISHED`
- `NotificationStatus`: `UNREAD`, `READ`

## Seed Data

Seed data duoc cai dat trong `DatabaseSeeder` va mac dinh khong chay. Bat seed bang bien moi truong:

```properties
SEED_ENABLED=true
SEED_ADMIN_PASSWORD=change-this-admin-password
SEED_TEACHER_PASSWORD=change-this-teacher-password
SEED_STUDENT_PASSWORD=change-this-student-password
```

Seeder tao cac du lieu co ban: admin, teacher, student, class, subject, class-subject-teacher assignment, question bank, question, exam va exam session.
