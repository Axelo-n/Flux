package com.example.flux.preferences

/** UI labels only. Stored categories, bank notifications and user notes keep their original text. */
fun translate(text: String, language: String = AppPreferences.state.value.language): String {
    if (language == "id") return mealLabels[text] ?: indonesianLabels[text] ?: text
    englishLabels[text]?.let { return it }
    for ((pattern, replacement) in dynamicLabels) {
        if (pattern.matches(text)) return pattern.replace(text, replacement)
    }
    return text
}

private val mealLabels = mapOf("Breakfast" to "Sarapan", "Lunch" to "Makan siang", "Dinner" to "Makan malam", "Snack" to "Snack", "Extra food" to "Makanan tambahan", "Food stock" to "Stok makanan", "Drinks" to "Minuman")
private val indonesianLabels = mapOf("Expense" to "Pengeluaran", "Income" to "Pemasukan", "Food and Beverages" to "Makanan & minuman", "Food & Beverages" to "Makanan & minuman", "Transportation" to "Transportasi", "Groceries and Shopping" to "Belanja", "Entertainment" to "Hiburan", "Account Transfer" to "Transfer rekening", "Other" to "Lainnya", "Cancel" to "Batal", "Refund" to "Pengembalian", "manual" to "manual", "Language" to "Bahasa", "Theme" to "Tema")
private val englishLabels = """
Ubah kategori | Change category
Pilih kategori | Choose category
Sesuaikan kategori transaksi ini. | Choose a category for this transaction.
Hapus pencarian | Clear search
Coba parser tanpa membuat transaksi | Try the parser without creating transactions
Pakai contoh blu | Use blu sample
Isi contoh untuk melihat hasil parser. | Enter a sample to preview the parser result.
Semua alokasi bulanan, dalam satu tempat. | All monthly allocations in one place.
Mingguan | Weekly
Bulanan tetap | Fixed monthly
Pocket mingguan | Weekly pockets
Pocket bulanan | Monthly pockets
Tambah pocket bulanan | Add monthly pocket
Nama pocket | Pocket name
Nominal per bulan | Monthly amount
Simpan pocket | Save pocket
Edit pocket | Edit pocket
Hapus pocket | Delete pocket
Hapus pocket? | Delete pocket?
Detail pocket | Pocket details
Tetap tiap bulan | Fixed each month
Nominal tetap setiap bulan, sampai lu ubah atau hapus. | A fixed amount every month until you edit or delete it.
Nominal tetap, otomatis masuk pembagian setiap bulan. | A fixed amount included in every month's allocation.
Hanya alokasi; saldo dan budget harian tetap sama. | Allocation only; your balance and daily budget stay the same.
Pocket ini akan dihapus dari semua pembagian bulanan. Transaksi tetap tersimpan. | This pocket will be removed from all monthly allocations. Transactions stay saved.
Tambahkan kos, tabungan, atau alokasi lain dengan nominal tetap setiap bulan. | Add rent, savings or other fixed monthly allocations.
Ketuk kartu untuk rincian. Ini kalkulator alokasi; tidak membuat transaksi atau memindahkan uang. | Tap a card for details. This allocation calculator doesn't create transactions or move money.
Nominal mengikuti budget harian. Tanggal sebelum budget pertama memakai pola pertama yang tersedia. | Amounts follow daily budgets. Dates before your first budget use the earliest available pattern.
Pocket tersimpan | Pocket saved
Pocket dihapus | Pocket deleted
Isi nama pocket (maksimal 80 karakter) | Enter a pocket name (up to 80 characters)
Nominal pocket harus positif | Pocket amount must be positive
Budget pocket | Pocket budget
Siapkan dana mingguan, pas sampai akhir bulan. | Plan weekly funds through the end of the month.
DANA BULAN INI | THIS MONTH'S FUNDS
pocket mingguan | weekly pockets
hari | days
Pocket minggu | Weekly pocket
Rincian harian | Daily breakdown
Hitung dana setiap minggu dalam sebulan | Calculate each week's funds for the month
Utility | Utilities
Senin–Minggu, dipotong batas bulan. Nominal mengikuti budget setiap tanggal; saldo dan extra tidak ikut dihitung. | Monday to Sunday, clipped to the month. Totals follow each day's budget, excluding balance and extra.
Atur budget harian dulu untuk menghitung pocket. | Set your daily budget to calculate pockets.
Ini kalkulator saja; tidak membuat transaksi atau memindahkan uang. Tanggal sebelum budget pertama memakai pola budget pertama yang tersedia. | This calculator doesn't create transactions or move money. Dates before the first budget use the earliest available weekly pattern.
Cashback menunggu transaksi asal | Cashback needs an original transaction
Pilih pengeluaran asal. Cashback mengurangi pengeluaran dan mengembalikan budget. | Choose the original expense. Cashback reduces spending and restores your budget.
Pilih transaksi asal | Choose original transaction
Cashback akan mengurangi nominal efektif transaksi yang dipilih. | Cashback reduces the selected transaction's effective amount.
Tidak ada pengeluaran yang sesuai. Periksa nominal dan tanggal transaksi. | No eligible expense. Check the amount and transaction date.
Pilih transaksi di Sistem untuk mengembalikan budget. | Choose a transaction in System to restore your budget.
Cashback ditandai selesai | Cashback marked complete
Sudah ditangani manual | Already handled manually
Cashback ditangani manual; transaksi tidak diubah. | Cashback handled manually; transactions unchanged.
Cashback ditautkan | Cashback linked
Cashback perlu ditautkan | Cashback needs linking
Cashback tidak ditemukan | Cashback not found
Cashback sudah diproses | Cashback already processed
Cashback belum dikenali | Cashback not recognized
Cashback identik sudah diproses | Identical cashback already processed
Transaksi asal tidak ditemukan | Original transaction not found
Cashback melebihi pengeluaran tersisa atau tanggal tidak sesuai | Cashback exceeds remaining expense or the date doesn't match
Pilih transaksi asal untuk mengembalikan saldo dan budget. | Choose the original transaction to restore balance and budget.
Cashback duplikat sudah diproses | Duplicate cashback already processed
Refund / cashback / cancel | Refund / cashback / cancel
Refund / cashback | Refund / cashback
Ada refund atau cashback? | Any refund or cashback?
Tambahkan refund / cashback | Add refund / cashback
Total refund + cashback (Rp) | Total refund + cashback (Rp)
Alasan refund / cashback · wajib | Refund / cashback reason · required
Dikembalikan penuh | Fully returned
Sistem | System
Preferensi | Preferences
Bahasa | Language
Tema | Theme
Putih | White
Hitam | Black
Aktifkan aturan | Enable rule
Catatan dari notifikasi | Note from notification
Abaikan notifikasi yang mengandung keyword ini. | Ignore notifications containing this keyword.
Atur kategori dan catatan untuk transaksi langganan. | Set the category and note for recurring transactions.
Kosongkan catatan untuk memakai nama merchant atau penerima dari notifikasi. | Leave the note blank to use the merchant or recipient from the notification.
Tutup | Close
Pilih bahasa | Choose language
Pilih tema | Choose theme
Tampilan seluruh aplikasi | Appearance throughout the app
Bahasa tampilan aplikasi | App display language
Hari ini | Today
Analisis | Analytics
Riwayat | History
Catat | Add
Catat manual | Add manually
Catat transaksi | Add transaction
Kembali | Back
blu aktif | blu active
Cek blu | Check blu
SISA JATAH HARI INI | DAILY BUDGET LEFT
Terpakai hari ini | Spent today
Extra tersedia | Available extra
Sisa hari sebelumnya | Left over from previous days
Melampaui jatah & extra | Beyond budget and extra
Saldo blu | blu balance
Berdasarkan catatan | Based on your records
Ruang belanja | Available to spend
Jatah + extra, dibatasi saldo | Budget + extra, capped by balance
Extra minus akan mengurangi sisa yang terkumpul berikutnya. | Negative extra reduces future accumulated savings.
Extra melebihi saldo. Periksa dana dan catatan transaksi. | Extra exceeds your balance. Check funds and transaction records.
Ritme belanja | Spending rhythm
Tujuh hari terakhir | Last seven days
Pengeluaran | Expense
Pemasukan | Income
Kini | Today
Garis kecil = jatah harian · Sisa hari ini masuk extra besok | Small line = daily budget · Today's remainder becomes extra tomorrow
Aktivitas hari ini | Today's activity
Hari baru, catatan baru | A fresh day, a fresh record
Transaksi blu akan muncul di sini. Bisa catat manual juga. | blu transactions appear here. You can also add them manually.
Habis | Used up
Mulai lebih tenang. | Start with peace of mind.
Satu rekening blu. Jatah yang jelas. Sisa yang jadi extra. | One blu account. A clear budget. Leftovers become extra.
01 · Saldo awal | 01 · Opening balance
02 · Jatah harian | 02 · Daily budget
Saldo blu saat ini (Rp) | Current blu balance (Rp)
Isi sesuai saldo rekening sekarang. Extra mulai dari Rp0, dan hari pertama mendapat jatah penuh. | Enter your current account balance. Extra starts at Rp0; your first day gets its full budget.
Pergantian hari | Day boundary
Zona waktu | Time zone
Zona waktu lain | Other time zone
Tutup zona waktu lain | Close time zone input
Budget harian | Daily budget
Atur jatah tiap hari tanpa mengubah perhitungan masa lalu. | Set each day's budget without changing past calculations.
Jatah harian | Daily allowances
Bisa berbeda tiap hari. Isi 0 untuk hari tanpa jatah. | Each day can be different. Enter 0 for days without a budget.
Samakan semua dengan Senin | Use Monday's amount for every day
Senin | Monday
Selasa | Tuesday
Rabu | Wednesday
Kamis | Thursday
Jumat | Friday
Sabtu | Saturday
Minggu | Sunday
Sen | Mon
Sel | Tue
Rab | Wed
Kam | Thu
Jum | Fri
Sab | Sat
Min | Sun
TOTAL JATAH SEMINGGU | TOTAL WEEKLY BUDGET
Lengkapi nominal yang valid untuk melihat total. | Enter valid amounts to see the total.
Sisa jatah masuk extra setelah hari berakhir. | Unspent budget becomes extra once the day ends.
Mulai berlaku | Effective from
Tanggal · YYYY-MM-DD | Date · YYYY-MM-DD
Pilih | Choose
Pilih tanggal | Choose date
Paling cepat besok. Jadwal pada tanggal yang sama diperbarui selama belum berlaku. Budget hari sebelumnya tetap sama. | Tomorrow at the earliest. Schedules on the same date can be updated before they take effect. Past budgets stay the same.
Menyimpan… | Saving…
Mulai pencatatan baru | Start fresh
Jadwalkan budget | Schedule budget
Riwayat kebijakan | Budget history
Edit jadwal | Edit schedule
Batalkan | Cancel
Batal | Cancel
Mulai dari nol? | Start from zero?
Hapus & mulai | Clear and start
Detail transaksi | Transaction details
Tercatat otomatis dari blu | Automatically recorded from blu
Pencatatan manual | Manual entry
Nominal | Amount
Nominal sebelum refund | Amount before refund
Pemasukan menambah saldo rekening. | Income increases your account balance.
Pengeluaran memakai jatah hari transaksi, lalu extra jika perlu. | Expenses use that day's budget, then extra if needed.
Detail catatan | Record details
Catatan · opsional | Note · optional
Contoh: nasgor malam | Example: dinner
Kategori | Category
Refund / cancel | Refund / cancellation
Ada pengembalian dana? | Was any money refunded?
Ringkas | Collapse
Tambahkan refund | Add refund
Refund mengurangi pengeluaran pada tanggal asal. Saldo, budget, dan extra ikut dihitung ulang. | A refund reduces the original day's expense. Balance, budget and extra are recalculated.
Total refund (Rp) · kosong jika tidak ada | Total refund (Rp) · leave empty if none
Refund penuh | Full refund
Alasan refund / cancel · wajib | Refund / cancellation reason · required
Simpan transaksi | Save transaction
Hapus transaksi | Delete transaction
Hapus transaksi? | Delete this transaction?
Saldo, budget, dan extra akan dihitung ulang. Gunakan refund jika transaksi dibatalkan agar riwayat tetap terlihat. | Balance, budget and extra will be recalculated. Use a refund for cancelled transactions to keep their history.
Hapus | Delete
Simpan | Save
Dibatalkan | Cancelled
Setiap transaksi, tersimpan rapi. | Every transaction, neatly recorded.
Cari catatan atau kategori | Search notes or categories
Semua | All
Belum ada transaksi yang cocok. | No matching transactions.
Kenali ritme dan arah pengeluaran lu. | Understand your spending rhythm and habits.
Total pengeluaran | Total expenses
Belum ada pengeluaran | No expenses yet
Selama bulan ini | This month
Tren harian | Daily trend
Per kategori | By category
Bulan sebelumnya | Previous month
Bulan berikutnya | Next month
Atur kebiasaan, pencatatan, dan data lu di satu tempat. | Manage budgets, recording and data in one place.
Sesuaikan jatah setiap hari | Customize each day's budget
Aturan otomatis & blacklist | Auto rules & blacklist
Kategori otomatis, tanpa notifikasi pengganggu | Automatic categories without unwanted notifications
Pencatatan blu | blu recording
● Terhubung | ● Connected
● Belum tersambung | ● Not connected
● Akses belum diberikan | ● Access not granted
Status ini mengikuti koneksi listener, bukan hanya izin Android. | This shows the actual listener connection, not just Android permissions.
Atur akses notifikasi | Manage notification access
Hubungkan ulang | Reconnect
Izinkan notifikasi layanan | Allow service notifications
Izinkan berjalan di background | Allow background operation
Saat tersambung ulang, Flux memeriksa notifikasi blu yang masih tersedia. Notifikasi yang sudah hilang perlu dicatat manual. | On reconnect, Flux checks blu notifications that are still available. Dismissed notifications need manual entries.
Koreksi manual | Manual adjustments
Sesuaikan saldo atau extra jika ada selisih catatan. | Adjust balance or extra to correct a discrepancy.
Tutup koreksi | Close adjustment
Buat koreksi | Add adjustment
Saldo | Balance
saldo | balance
Tambah | Increase
Kurangi | Decrease
Nominal (Rp) | Amount (Rp)
Alasan koreksi | Adjustment reason
Koreksi saldo hanya mengubah saldo. Koreksi extra hanya mengubah extra, bukan pemasukan atau pengeluaran. | Balance adjustments only change balance. Extra adjustments only change extra, not income or expenses.
Simpan koreksi | Save adjustment
Batalkan koreksi | Undo adjustment
Backup & data | Backup & data
Ekspor backup | Export backup
Restore backup | Restore backup
Mulai ulang dari nol | Start over from zero
Backup menyimpan transaksi, aturan, budget, dan koreksi. Restore mengganti data setelah validasi; log notifikasi tidak ikut diekspor. | Backups include transactions, rules, budgets and adjustments. Restore replaces data after validation; notification logs are excluded.
Aktivitas notifikasi · 100 terbaru | Notification activity · latest 100
Belum ada notifikasi blu sejak sistem dimulai. | No blu notifications since setup.
Tercatat | Recorded
Diabaikan | Ignored
Perlu diperiksa | Needs review
Ringkas aktivitas | Collapse activity
Ganti data dengan backup? | Replace data with this backup?
Data sekarang akan diganti seluruhnya jika backup valid. Backup versi lama Flux tidak didukung. | Current data will be replaced if the backup is valid. Old Flux backup versions are not supported.
Aturan otomatis | Auto rules
Nama langganan jadi catatan. Notifikasi pengganggu berhenti di sini. | Turn regular merchants into notes. Stop unwanted notifications here.
Cari keyword atau catatan | Search keywords or notes
+ Tambah blacklist | + Add blacklist
+ Tambah aturan | + Add rule
Blacklist diperiksa lebih dulu. Keyword cocok → notifikasi diabaikan. | Blacklist is checked first. Matching keyword → notification ignored.
Pencocokan mengandung keyword, tanpa membedakan huruf besar/kecil. Aturan terbaru mendapat prioritas. | Matches contain the keyword, ignoring case. Newer rules take priority.
Belum ada aturan. Mulai dari tempat yang paling sering lu pakai. | No rules yet. Start with a merchant you use often.
Tidak ada aturan yang cocok. | No matching rules.
Abaikan notifikasi | Ignore notification
AKTIF | ACTIVE
NONAKTIF | INACTIVE
Uji | Test
Tutup | Close
Uji contoh notifikasi | Test sample notification
Tutup contoh notifikasi | Close sample notification
Preview pencatatan | Recording preview
Judul notifikasi | Notification title
Isi notifikasi | Notification content
Catatan kosong | Empty note
catatan kosong | empty note
Belum dikenali; akan masuk log untuk diperiksa. | Not recognized; logged for review.
Preview tidak menyimpan transaksi. | Preview does not save a transaction.
Transaksi yang sudah tercatat tetap tersimpan. | Existing transactions remain unchanged.
Blacklist keyword | Blacklist keyword
Aturan pencatatan | Recording rule
Keyword · contoh: nama penjual | Keyword · example: merchant name
Catatan otomatis · opsional | Automatic note · optional
Contoh: blugether. Semua notifikasi yang mengandung keyword ini akan diabaikan. | Example: blugether. All notifications containing this keyword will be ignored.
Keyword mengubah kategori dan catatan. Nominal serta jenis transaksi tetap dibaca dari notifikasi. | Keywords set the category and note. Amount and transaction type still come from the notification.
Sistem baru siap | Fresh setup ready
Budget baru dijadwalkan | New budget scheduled
Jadwal budget dibatalkan | Budget schedule cancelled
Transaksi tersimpan | Transaction saved
Transaksi dihapus | Transaction deleted
Aturan tersimpan | Rule saved
Aturan dihapus | Rule deleted
Koreksi tersimpan | Adjustment saved
Koreksi dihapus | Adjustment deleted
Tidak berhasil menyimpan. Coba lagi. | Couldn't save. Try again.
Backup tidak valid; data saat ini tetap aman. | Invalid backup; current data is safe.
Izin notifikasi diberikan | Notification permission granted
Izin notifikasi belum diberikan | Notification permission not granted
Backup tersimpan | Backup saved
Backup gagal disimpan | Couldn't save backup
Backup tidak bisa dibaca (maksimal 5 MB) | Couldn't read backup (maximum 5 MB)
Transaksi tercatat | Transaction recorded
Pencatatan otomatis | Automatic recording
Flux · blu terhubung | Flux · blu connected
Pencatatan otomatis siap. Ketuk untuk melihat keuangan. | Automatic recording ready. Tap to view your finances.
Belum bisa terhubung. Periksa akses notifikasi di Android. | Couldn't connect. Check Android notification access.
Notifikasi layanan belum aktif. Periksa izin notifikasi. | Service notifications are not active. Check notification permission.
Pencatatan gagal. Periksa riwayat dan koreksi manual. | Recording failed. Check history and add a manual correction.
Notifikasi identik dalam 2 menit. Mungkin duplikat; catat manual jika ini transaksi lain. | Identical notification within two minutes. Possible duplicate; add manually if this is a separate transaction.
Nominal atau jenis transaksi tidak dikenali. Tambahkan manual bila perlu. | Amount or transaction type not recognized. Add manually if needed.
""".trimIndent().lines().filter { " | " in it }.associate { val pair = it.split(" | ", limit = 2); pair[0] to pair[1] }

private val dynamicLabels = listOf(
    Regex("^(.*) transaksi tercatat$") to "\$1 transactions recorded",
    Regex("^Dari jatah (.*)$") to "From a budget of \$1",
    Regex("^(.*) memakai extra$") to "\$1 uses extra",
    Regex("^(.*) persen budget terpakai$") to "\$1 percent of budget spent",
    Regex("^(.*)% dari pengeluaran$") to "\$1% of expenses",
    Regex("^(.*) kategori bulan ini$") to "\$1 categories this month",
    Regex("^(.*) · Zona waktu dikunci selama periode pencatatan ini\\.$") to "\$1 · Time zone is fixed for this recording period.",
    Regex("^Mulai (\\d{4}-\\d{2}-\\d{2})$") to "Effective \$1",
    Regex("^blu · (.*) · mulai (.*)$") to "blu · \$1 · since \$2",
    Regex("^Notifikasi terakhir: (.*)$") to "Last notification: \$1",
    Regex("^Lihat semua (.*) notifikasi$") to "View all \$1 notifications",
    Regex("^Pengeluaran efektif: (.*)$") to "Effective expense: \$1",
    Regex("^Diabaikan oleh '(.*)'$") to "Ignored by '\$1'",
    Regex("^Hapus '(.*)'\\?$") to "Delete '\$1'?",
    Regex("^Hapus (.*)$") to "Delete \$1",
    Regex("^Semua transaksi, aturan, koreksi, dan log lama akan dihapus\\. Saldo awal (.*); extra Rp0\\. Backup dulu jika masih dibutuhkan\\.$") to "All previous transactions, rules, adjustments and logs will be deleted. Opening balance \$1; extra Rp0. Back up first if needed.",
    Regex("^Tercatat otomatis dari blu · (.*)$") to "Automatically recorded from blu · \$1",
    Regex("^Pencatatan manual · (.*)$") to "Manual entry · \$1"
)
