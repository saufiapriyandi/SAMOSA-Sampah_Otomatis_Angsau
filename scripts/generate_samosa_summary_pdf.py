import time
from pathlib import Path

import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "pdf"
TMP_DIR = ROOT / "tmp" / "pdfs"
PDF_PATH = OUTPUT_DIR / "samosa_ringkasan_aplikasi.pdf"
PNG_PATH = TMP_DIR / "samosa_ringkasan_aplikasi_page1.png"


TITLE = "Ringkasan Aplikasi SAMOSA"
SUBTITLE = (
    "Sampah Otomatis Angsau | dirangkum dari bukti kode di repo | bahasa Indonesia"
)

LEFT_SECTIONS = [
    (
        "Apa Itu",
        [
            "SAMOSA adalah aplikasi Android native untuk memantau kondisi tempat sampah di lingkungan UPTD SDN 4 Angsau.",
            "Versi repo ini menampilkan dashboard status, detail, laporan, dan notifikasi dengan data lokal dan simulasi.",
        ],
    ),
    (
        "Untuk Siapa",
        [
            "Persona utama yang paling jelas di repo adalah Kepala Sekolah atau admin sekolah; hal ini tampak pada copy UI, profil pengguna, dan alur monitoring utama.",
        ],
    ),
    (
        "Fitur Utama",
        [
            "- Masuk dengan akun lokal <b>admin123/admin123</b> atau Google Sign-In melalui Firebase Auth.",
            "- Dashboard menampilkan total tong, jumlah penuh, jumlah aman, pencarian lokasi, dan status aman/waspada/penuh.",
            "- Peringatan visual muncul saat ada tong penuh, disertai indikator data yang sudah lama.",
            "- Halaman detail menampilkan BIN-ID, tanggal, dan riwayat persentase per hari sekolah.",
            "- Manajemen tong mendukung tambah, edit, ubah persentase simulasi, serta aktif/nonaktifkan lokasi.",
            "- Laporan harian dan mingguan dapat ditinjau lalu dibagikan sebagai file teks.",
            "- Notifikasi langsung dan pengingat berkala aktif untuk tong yang berstatus penuh.",
        ],
    ),
]

RIGHT_SECTIONS = [
    (
        "Cara Kerja",
        [
            "- UI utama terdiri dari MainActivity, DashboardActivity, DetailActivity, ProfileActivity, TutorialActivity, BinManagementActivity, dan ReportActivity.",
            "- MainActivity menangani login lokal serta Google Sign-In, lalu autentikasi ke FirebaseAuth.",
            "- DashboardActivity mengamati DashboardViewModel; ViewModel memanggil TempatSampahRepository untuk memuat data.",
            "- Implementasi repo yang dipakai saat ini adalah MockTempatSampahRepository, yang membaca TempatSampahLocalStore.",
            "- TempatSampahLocalStore menyimpan daftar bin sebagai JSON di SharedPreferences dan melakukan seed data default bila kosong.",
            "- Detail dan laporan memanfaatkan TempatSampahHistoryHelper dan LaporanSampahHelper untuk membentuk riwayat serta ringkasan periode dari data simulasi.",
            "- TempatSampahNotificationHelper memakai AlarmManager, BroadcastReceiver, dan NotificationManager untuk notifikasi dan pengingat berkala.",
        ],
    ),
    (
        "Mulai Cepat",
        [
            "1. Instal Android Studio, Android SDK 36, dan JDK 11+, lalu buka folder proyek ini.",
            "2. Biarkan Gradle sync; plugin Google Services aktif dan file <b>app/google-services.json</b> sudah tersedia.",
            "3. Jalankan ke emulator atau perangkat Android API 24+ dari Android Studio, atau pakai <b>gradlew.bat installDebug</b> jika <b>JAVA_HOME</b> sudah disetel.",
            "4. Masuk memakai <b>admin123/admin123</b> atau tombol Google Sign-In pada layar awal.",
        ],
    ),
]

NOT_FOUND_ITEMS = [
    "- CRUD runtime Firebase Realtime Database, endpoint sensor, dan sinkronisasi backend: Not found in repo.",
    "- README atau panduan setup resmi proyek: Not found in repo.",
]


def build_styles():
    sample = getSampleStyleSheet()
    return {
        "title": ParagraphStyle(
            "Title",
            parent=sample["Title"],
            fontName="Helvetica-Bold",
            fontSize=20,
            leading=24,
            textColor=colors.HexColor("#114B36"),
            spaceAfter=0,
        ),
        "subtitle": ParagraphStyle(
            "Subtitle",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=8.6,
            leading=10.2,
            textColor=colors.HexColor("#46645A"),
            spaceAfter=0,
        ),
        "section": ParagraphStyle(
            "Section",
            parent=sample["Heading4"],
            fontName="Helvetica-Bold",
            fontSize=11,
            leading=12,
            textColor=colors.HexColor("#166A4A"),
            spaceAfter=0,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=8.25,
            leading=10.1,
            textColor=colors.HexColor("#1F2D2A"),
            spaceAfter=0,
        ),
        "note_title": ParagraphStyle(
            "NoteTitle",
            parent=sample["Heading5"],
            fontName="Helvetica-Bold",
            fontSize=10.2,
            leading=11.5,
            textColor=colors.HexColor("#7A2E16"),
            spaceAfter=0,
        ),
        "note_body": ParagraphStyle(
            "NoteBody",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=7.9,
            leading=9.5,
            textColor=colors.HexColor("#4A352C"),
            spaceAfter=0,
        ),
        "footer": ParagraphStyle(
            "Footer",
            parent=sample["BodyText"],
            fontName="Helvetica",
            fontSize=6.7,
            leading=8,
            textColor=colors.HexColor("#6B7B76"),
            spaceAfter=0,
        ),
    }


def draw_paragraph(pdf: canvas.Canvas, text: str, style: ParagraphStyle, x: float, y: float, width: float, gap: float) -> float:
    paragraph = Paragraph(text, style)
    _, height = paragraph.wrap(width, 1000)
    paragraph.drawOn(pdf, x, y - height)
    return y - height - gap


def draw_section(
    pdf: canvas.Canvas,
    styles: dict,
    title: str,
    paragraphs: list[str],
    x: float,
    y: float,
    width: float,
) -> float:
    y = draw_paragraph(pdf, title, styles["section"], x, y, width, 6)
    pdf.setStrokeColor(colors.HexColor("#20B273"))
    pdf.setLineWidth(1)
    pdf.line(x, y + 2, x + width, y + 2)
    y -= 6
    for paragraph in paragraphs:
        y = draw_paragraph(pdf, paragraph, styles["body"], x, y, width, 4.5)
    return y - 4


def draw_not_found_box(
    pdf: canvas.Canvas,
    styles: dict,
    items: list[str],
    x: float,
    y_top: float,
    width: float,
) -> None:
    content_heights = []
    title_paragraph = Paragraph("Informasi Yang Belum Ada", styles["note_title"])
    _, title_height = title_paragraph.wrap(width - 18, 1000)
    content_heights.append(title_height)

    bullet_paragraphs = [Paragraph(item, styles["note_body"]) for item in items]
    for paragraph in bullet_paragraphs:
        _, height = paragraph.wrap(width - 18, 1000)
        content_heights.append(height)

    total_height = sum(content_heights) + 10 + 6 * (len(content_heights) - 1) + 14

    pdf.setFillColor(colors.HexColor("#FFF4EF"))
    pdf.setStrokeColor(colors.HexColor("#F2C3AF"))
    pdf.roundRect(x, y_top - total_height, width, total_height, 8, fill=1, stroke=1)

    inner_x = x + 9
    y = y_top - 8
    y = draw_paragraph(pdf, "Informasi Yang Belum Ada", styles["note_title"], inner_x, y, width - 18, 5)
    for item in items:
        y = draw_paragraph(pdf, item, styles["note_body"], inner_x, y, width - 18, 4)


def render_pdf() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    TMP_DIR.mkdir(parents=True, exist_ok=True)

    page_width, page_height = A4
    pdf = canvas.Canvas(str(PDF_PATH), pagesize=A4)
    styles = build_styles()

    pdf.setTitle(TITLE)

    margin_x = 34
    top_y = page_height - 34
    content_width = page_width - (margin_x * 2)
    gap = 18
    col_width = (content_width - gap) / 2

    pdf.setFillColor(colors.HexColor("#F3FBF7"))
    pdf.roundRect(margin_x, top_y - 76, content_width, 76, 12, fill=1, stroke=0)
    pdf.setFillColor(colors.HexColor("#20B273"))
    pdf.roundRect(margin_x, top_y - 76, 10, 76, 12, fill=1, stroke=0)

    title_y = top_y - 18
    draw_paragraph(pdf, TITLE, styles["title"], margin_x + 22, title_y, content_width - 30, 2)
    draw_paragraph(pdf, SUBTITLE, styles["subtitle"], margin_x + 22, title_y - 28, content_width - 30, 0)

    column_top = top_y - 96
    left_x = margin_x
    right_x = margin_x + col_width + gap

    left_y = column_top
    for section_title, paragraphs in LEFT_SECTIONS:
        left_y = draw_section(pdf, styles, section_title, paragraphs, left_x, left_y, col_width)

    right_y = column_top
    for section_title, paragraphs in RIGHT_SECTIONS:
        right_y = draw_section(pdf, styles, section_title, paragraphs, right_x, right_y, col_width)

    not_found_top = min(left_y, right_y) - 4
    draw_not_found_box(pdf, styles, NOT_FOUND_ITEMS, margin_x, not_found_top, content_width)

    footer_text = (
        "Basis bukti repo: AndroidManifest, MainActivity, DashboardActivity, DashboardViewModel, "
        "TempatSampahLocalStore, TempatSampahNotificationHelper, ReportActivity, "
        "LaporanSampahHelper, dan google-services.json."
    )
    draw_paragraph(pdf, footer_text, styles["footer"], margin_x, 48, content_width, 0)

    pdf.save()


def validate_pdf() -> None:
    reader = PdfReader(str(PDF_PATH))
    if len(reader.pages) != 1:
        raise RuntimeError(f"PDF harus 1 halaman, tetapi ditemukan {len(reader.pages)} halaman.")


def resolve_png_path() -> Path:
    if not PNG_PATH.exists():
        return PNG_PATH

    try:
        PNG_PATH.unlink()
        return PNG_PATH
    except PermissionError:
        pass

    for index in range(2, 10):
        candidate = TMP_DIR / f"samosa_ringkasan_aplikasi_page1_{index}.png"
        if not candidate.exists():
            return candidate

    return TMP_DIR / f"samosa_ringkasan_aplikasi_page1_{int(time.time())}.png"


def render_png() -> Path:
    document = fitz.open(str(PDF_PATH))
    page = document.load_page(0)
    pixmap = page.get_pixmap(matrix=fitz.Matrix(2, 2), alpha=False)
    png_path = resolve_png_path()
    pixmap.save(str(png_path))
    document.close()
    return png_path


if __name__ == "__main__":
    render_pdf()
    validate_pdf()
    png_path = render_png()
    print(PDF_PATH)
    print(png_path)
