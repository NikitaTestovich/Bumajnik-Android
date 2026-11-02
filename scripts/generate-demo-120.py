"""Generate synthetic, reproducible version 1.20 CSV files and expected totals."""
import calendar
import csv
import json
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

ROOT = Path(__file__).resolve().parents[1] / ".superpowers" / "fixtures-1.20"
CATEGORIES = ("Продукты", "Жильё и коммунальные услуги", "Транспорт", "Связь", "Здоровье", "Образование", "Одежда", "Кафе и рестораны", "Развлечения", "Бытовые товары", "Другое")
PEOPLE = (("Анна Пример", "BYN"), ("Иван Пример", "BYN"), ("Мария Пример", "USD"))


def generate():
    ROOT.mkdir(parents=True, exist_ok=True)
    totals = {}
    for person_index, (name, currency) in enumerate(PEOPLE):
        monthly = {}
        path = ROOT / f"person-{person_index + 1}.csv"
        with path.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\r\n")
            writer.writerow(("owner", "record_id", "date", "category", "income", "expense", "currency", "comment"))
            for month in (7, 8, 9):
                income = expense = 0
                for item in range(140):
                    day = item % calendar.monthrange(2026, month)[1] + 1
                    is_income = item % 14 == 0
                    cents = (35000 + item * 179 + person_index * 307) if is_income else (83 + item * 37 + person_index * 19)
                    amount = f"{cents // 100}.{cents % 100:02d}"
                    income += cents if is_income else 0
                    expense += 0 if is_income else cents
                    category = "" if is_income else CATEGORIES[(item + person_index) % len(CATEGORIES)]
                    comment = ("Зарплата" if is_income else f'Покупка {item}, "пример"') if item % 3 else ""
                    record_id = str(uuid5(NAMESPACE_URL, f"finance-demo-120-{person_index}-{month}-{item}"))
                    writer.writerow((name, record_id, f"{day:02d}-{month:02d}-2026", category, amount if is_income else "", "" if is_income else amount, currency, comment))
                monthly[str(month)] = {"income_minor": income, "expense_minor": expense, "operations": 140}
        totals[name] = {"currency": currency, "monthly": monthly, "file": path.name}
    (ROOT / "expected.json").write_text(json.dumps(totals, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated 3 fictional users, 1260 operations and expected totals: {ROOT}")


if __name__ == "__main__":
    generate()
