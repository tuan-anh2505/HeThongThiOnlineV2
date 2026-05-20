# HeThongThiOnlineV2 Backend

## Import cau hoi tu file TXT

Endpoint:

```http
POST /api/question-banks/{bankId}/import
Content-Type: multipart/form-data
Authorization: Bearer <jwt>
```

Form field:

```text
file=<questions.txt>
```

Quyen truy cap:

- `ADMIN` duoc import vao moi ngan hang cau hoi dang `ACTIVE`.
- `TEACHER` chi duoc import vao ngan hang cau hoi do minh so huu.
- `STUDENT` khong duoc truy cap API quan ly/import cau hoi.

Nguyen tac xu ly:

- Hien tai ho tro on dinh file `.txt` ma hoa UTF-8.
- DOCX/PDF chua duoc bat vi project chua co dependency parse chuyen dung nhu Apache POI/PDFBox; co the bo sung sau ma khong doi contract API.
- Moi file gom nhieu block `[QUESTION]`.
- Neu co bat ky cau hoi nao loi, API tra `400` kem danh sach loi theo `questionIndex` va `lineNumber`.
- Khong luu mot phan. He thong chi insert khi toan bo file parse va validate thanh cong.

Format mau:

```text
[QUESTION]
TYPE=MULTIPLE_CHOICE
CONTENT=Cau hoi o day?
SCORE=1
DIFFICULTY=EASY
TOPIC=Chuong 1
A=Dap an A
B=Dap an B
C=Dap an C
D=Dap an D
CORRECT=A

[QUESTION]
TYPE=TRUE_FALSE
CONTENT=Java la ngon ngu lap trinh huong doi tuong.
SCORE=1
DIFFICULTY=EASY
CORRECT=true

[QUESTION]
TYPE=FILL_BLANK
CONTENT=HTTP la viet tat cua ______
SCORE=1
DIFFICULTY=MEDIUM
ACCEPTED=HyperText Transfer Protocol|Hyper Text Transfer Protocol
IGNORE_CASE=true
IGNORE_ACCENT=true
TRIM_SPACE=true

[QUESTION]
TYPE=MATCHING
CONTENT=Noi khai niem dung
SCORE=2
DIFFICULTY=MEDIUM
PAIR=HTTP=>Giao thuc truyen sieu van ban
PAIR=IP=>Giao thuc Internet
```

Gia tri enum:

- `TYPE`: `TRUE_FALSE`, `MULTIPLE_CHOICE`, `FILL_BLANK`, `MATCHING`
- `DIFFICULTY`: `EASY`, `MEDIUM`, `HARD`
- `STATUS`: `ACTIVE`, `INACTIVE` (khong bat buoc, mac dinh `ACTIVE`)
