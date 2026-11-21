package com.sukhoramen.cs.data

object DemoFixtures {
    fun interactions(now: Long = System.currentTimeMillis()): List<Interaction> {
        fun item(
            id: String, sender: String, handle: String, initials: String, platform: Platform,
            type: InteractionType, minutes: Int, text: String, completed: Boolean = false,
            previous: String? = null, restriction: String? = null,
        ): Interaction {
            val messages = buildList {
                if (previous != null) {
                    add(Message("$id-1", previous, false, now - (minutes + 12) * 60_000L))
                    add(Message("$id-2", "Halo, Kak! Terima kasih sudah menghubungi Sukho Ramen 🍜 Ada yang bisa kami bantu?", true, now - (minutes + 10) * 60_000L))
                }
                add(Message("$id-last", text, false, now - minutes * 60_000L))
            }
            return Interaction(
                id, sender, handle, initials, platform, type, messages, completed,
                post = if (type == InteractionType.COMMENT) PostContext(
                    "Semangkuk hangat, hari yang lebih baik. Cobain Sukho Signature Ramen dengan kuah gurih dan topping favoritmu! 🍜",
                    if (platform == Platform.INSTAGRAM) "Posting Instagram" else "Video TikTok",
                ) else null,
                replyRestriction = restriction,
            )
        }
        return listOf(
            item("ig-dm-nadia", "Nadia Putri", "nadiaputri", "NP", Platform.INSTAGRAM, InteractionType.DM, 2,
                "Kak, kalau untuk 8 orang bisa reservasi dulu nggak ya? 😊", previous = "Halo, mau tanya untuk makan bareng teman kantor."),
            item("tt-comment-rizky", "Rizky Pratama", "rizkyprtm", "RP", Platform.TIKTOK, InteractionType.COMMENT, 7,
                "Ini yang kuah creamy namanya apa? Kelihatan enak banget! 🍜"),
            item("ig-comment-dinda", "Dinda Amelia", "dinda.amelia", "DA", Platform.INSTAGRAM, InteractionType.COMMENT, 14,
                "Ada pilihan ramen yang nggak pedas buat anak-anak?"),
            item("tt-dm-arya", "Arya Saputra", "aryasptr", "AS", Platform.TIKTOK, InteractionType.DM, 23,
                "Sukho buka sampai jam berapa hari ini, Kak?", previous = "Hai Sukho!"),
            item("ig-dm-farah", "Farah Aulia", "farahaulia", "FA", Platform.INSTAGRAM, InteractionType.DM, 38,
                "Mau tanya, tersedia menu vegetarian nggak ya?"),
            item("tt-comment-kevin", "Kevin Wijaya", "kevinwj", "KW", Platform.TIKTOK, InteractionType.COMMENT, 65,
                "Besok langsung ke sini sih. Parkir mobilnya luas, Kak?"),
            item("ig-comment-salsa", "Salsa Nabila", "salsanbl", "SN", Platform.INSTAGRAM, InteractionType.COMMENT, 92,
                "Promo paket berdua masih ada nggak? Mau ajak sahabat ✨"),
            item("tt-dm-bima", "Bima Aditya", "bimaaditya", "BA", Platform.TIKTOK, InteractionType.DM, 130,
                "Bisa pesan takeaway langsung di outlet?"),
            item("ig-dm-anisa", "Anisa Rahma", "anisarhm", "AR", Platform.INSTAGRAM, InteractionType.DM, 180,
                "Terima kasih, Kak! Sampai ketemu nanti sore ya 🙌", completed = true, previous = "Apakah bisa reservasi jam 5?"),
            item("tt-comment-fajar", "Fajar Maulana", "fajarm", "FM", Platform.TIKTOK, InteractionType.COMMENT, 220,
                "Sudah nyobain kemarin. Oishii banget!", completed = true),
            item("ig-comment-tasya", "Tasya Kirana", "tasyakrn", "TK", Platform.INSTAGRAM, InteractionType.COMMENT, 300,
                "Lokasinya yang di mana, Kak?", completed = true),
            item("ig-dm-rafi", "Rafi Ramadhan", "rafirmd", "RR", Platform.INSTAGRAM, InteractionType.DM, 2_880,
                "Halo, mau tanya pesanan kemarin.", restriction = "Batas waktu balasan DM sudah berakhir (simulasi). Balasan melalui API tidak tersedia."),
        )
    }

    fun incoming(now: Long = System.currentTimeMillis()) = Interaction(
        id = "demo-live-event", sender = "Maya Lestari", handle = "mayalstr", initials = "ML",
        platform = Platform.INSTAGRAM, type = InteractionType.DM,
        messages = listOf(Message("demo-live-message", "Halo! Ada rekomendasi ramen untuk pertama kali coba? 🍜", false, now)),
    )
}

