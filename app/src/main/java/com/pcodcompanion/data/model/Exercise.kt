package com.pcodcompanion.data.model

data class Exercise(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val emoji: String,
    val description: String,
    val instructions: List<String>
)

object PredefinedMiniActions {
    val list = listOf(
        Exercise(
            id = "breathing_2",
            name = "Box Breathing",
            durationMinutes = 2,
            emoji = "🌬️",
            description = "A short breathwork session to calm your nervous system.",
            instructions = listOf(
                "Inhale slowly for 4 seconds",
                "Hold your breath for 4 seconds",
                "Exhale slowly for 4 seconds",
                "Hold empty for 4 seconds",
                "Repeat the cycle until the timer ends"
            )
        ),
        Exercise(
            id = "stretch_5",
            name = "Quick Stretch",
            durationMinutes = 5,
            emoji = "🤸",
            description = "Gentle micro-stretches to release tension.",
            instructions = listOf(
                "Neck rolls — 30 sec each direction",
                "Shoulder shrugs — 30 sec",
                "Standing forward fold — 1 min",
                "Side bends — 30 sec each side",
                "Wrist & ankle rotations — 1 min"
            )
        ),
        Exercise(
            id = "relax_3",
            name = "Relaxation",
            durationMinutes = 3,
            emoji = "🌿",
            description = "A short body-scan to soften and release stress.",
            instructions = listOf(
                "Sit or lie comfortably and close your eyes",
                "Soften your jaw and shoulders",
                "Notice your breath without changing it",
                "Scan from head to toes, releasing each area",
                "Stay until the timer ends"
            )
        )
    )
}

object PredefinedExercises {
    val list = listOf(
        Exercise(
            id = "walk_30",
            name = "Brisk Walk",
            durationMinutes = 30,
            emoji = "🏃‍♀️",
            description = "A moderate-intensity walk to improve insulin sensitivity and cardiovascular health.",
            instructions = listOf(
                "Maintain a steady, brisk pace.",
                "Keep your posture straight and swing your arms.",
                "Ensure you're breathing rhythmically.",
                "Hydrate before and after the walk."
            )
        ),
        Exercise(
            id = "yoga_15",
            name = "PCOD Relief Yoga",
            durationMinutes = 15,
            emoji = "🧘‍♀️",
            description = "Gentle yoga poses focusing on pelvic blood flow and stress reduction.",
            instructions = listOf(
                "Butterfly Pose (Baddha Konasana) - 3 mins",
                "Cobra Pose (Bhujangasana) - 3 mins",
                "Cat-Cow Stretch (Marjaryasana) - 3 mins",
                "Child's Pose (Balasana) - 3 mins",
                "Corpse Pose (Savasana) - 3 mins"
            )
        ),
        Exercise(
            id = "stretch_10",
            name = "Full Body Stretch",
            durationMinutes = 10,
            emoji = "🤸‍♀️",
            description = "Dynamic and static stretches to relieve muscle tension and improve flexibility.",
            instructions = listOf(
                "Neck rolls and shoulder shrugs - 2 mins",
                "Arm and chest stretches - 2 mins",
                "Torso twists - 2 mins",
                "Hamstring and quad stretches - 2 mins",
                "Calf raises and ankle rotations - 2 mins"
            )
        )
    )
}
