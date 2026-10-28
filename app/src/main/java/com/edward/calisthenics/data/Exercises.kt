package com.edward.calisthenics.data

import com.edward.calisthenics.data.Measure.REPS
import com.edward.calisthenics.data.Measure.TIME

enum class Category(val title: String) {
    WARMUP("Разминка"),
    LEGS("Ноги"),
    PUSH("Толчок"),
    PULL("Тяга"),
    CORE("Пресс и кор"),
    CARDIO("Кардио"),
    STRETCH("Растяжка"),
}

enum class Equip(val title: String) {
    NONE("Без инвентаря"),
    BAR("Турник"),
    BENCH("Скамья, лежак или бордюр"),
}

data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val measure: Measure,
    val muscles: String,
    val cues: List<String>,
    val mistakes: List<String>,
    /** Id анимации в moves.json. */
    val anim: String = id,
    val equip: Equip = Equip.NONE,
    val easier: String? = null,
    val harder: String? = null,
    /** Замена, если нужного инвентаря нет. */
    val alt: String? = null,
    /** Подпись к числу повторений, например «всего, обе ноги». */
    val repNote: String? = null,
    /** Метаболический эквивалент для оценки калорий. */
    val met: Double = 5.0,
    val secPerRep: Double = 3.0,
    /** Для кардио: объём не масштабируется уровнем сложности. */
    val fixedVolume: Boolean = false,
)

object Exercises {

    val all: List<Exercise> = listOf(
        // ---------- разминка ----------
        Exercise(
            "march", "Марш на месте", Category.WARMUP, TIME, "Ноги, разогрев",
            listOf("Поднимайте колени до уровня таза", "Руки работают в противоход ногам", "Спина прямая, дыхание ровное"),
            listOf("Сутулиться и смотреть в пол"),
            harder = "high_knees", met = 3.5,
        ),
        Exercise(
            "arm_circles", "Круги руками", Category.WARMUP, TIME, "Плечи",
            listOf("Руки прямые, в стороны на уровне плеч", "Небольшие круги вперёд, потом назад", "Плечи опущены, не поднимайте их к ушам"),
            listOf("Слишком резкие махи"),
            met = 2.5,
        ),
        Exercise(
            "step_jacks", "Шаги в стороны с руками вверх", Category.WARMUP, TIME, "Всё тело, разогрев",
            listOf("Шаг в сторону и одновременно руки над головой", "Возврат в исходное, затем в другую сторону", "Мягко, без прыжков: бережём колени"),
            listOf("Задерживать дыхание"),
            harder = "jumping_jacks", met = 4.0,
        ),
        Exercise(
            "jumping_jacks", "Прыжки «звёздочка»", Category.WARMUP, TIME, "Всё тело, кардио",
            listOf("Прыжок: ноги в стороны, руки над головой", "Приземляйтесь мягко на переднюю часть стопы", "Держите темп, который можете поддерживать"),
            listOf("Приземляться на прямые колени"),
            easier = "step_jacks", met = 7.0,
        ),
        Exercise(
            "inchworm", "Гусеница", Category.WARMUP, REPS, "Задняя поверхность бедра, плечи, кор",
            listOf("Наклонитесь и поставьте ладони на коврик", "Перебирая руками, выйдите в планку", "Так же вернитесь к ногам и выпрямитесь"),
            listOf("Проваливать поясницу в планке", "Сгибать колени сильно: допустимо чуть-чуть"),
            met = 4.0, secPerRep = 7.0,
        ),

        // ---------- ноги ----------
        Exercise(
            "squat", "Приседания", Category.LEGS, REPS, "Квадрицепсы, ягодицы",
            listOf("Стопы на ширине плеч, носки слегка врозь", "Таз назад и вниз, как будто садитесь на стул", "Колени смотрят туда же, куда носки", "Пятки не отрываются от пола"),
            listOf("Колени заваливаются внутрь", "Округлять спину внизу"),
            harder = "squat_jump", met = 5.0,
        ),
        Exercise(
            "squat_jump", "Приседания с выпрыгиванием", Category.LEGS, REPS, "Ноги, ягодицы, кардио",
            listOf("Присед до параллели, затем мощный прыжок вверх", "Руки помогают замахом", "Приземление мягкое, сразу в следующий присед"),
            listOf("Жёсткое приземление на прямые ноги"),
            easier = "squat", met = 8.0, secPerRep = 2.5,
        ),
        Exercise(
            "squat_hold", "Удержание приседа", Category.LEGS, TIME, "Квадрицепсы, ягодицы",
            listOf("Сядьте до параллели бёдер с полом", "Руки вперёд для баланса", "Вес на пятках, спина ровная"),
            listOf("Поднимать таз выше, чтобы было легче"),
            met = 5.0,
        ),
        Exercise(
            "reverse_lunge", "Выпады назад", Category.LEGS, REPS, "Ягодицы, квадрицепсы",
            listOf("Шаг назад, колено задней ноги почти касается пола", "Переднее колено над стопой", "Отталкивайтесь передней ногой, чередуйте ноги"),
            listOf("Колено передней ноги уходит далеко за носок", "Наклон корпуса вперёд"),
            harder = "jump_lunge", repNote = "всего, на обе ноги", met = 5.0,
        ),
        Exercise(
            "jump_lunge", "Выпады в прыжке", Category.LEGS, REPS, "Ноги, ягодицы, кардио",
            listOf("Из выпада прыжок со сменой ног в воздухе", "Мягкое приземление сразу в выпад", "Корпус вертикально"),
            listOf("Удар коленом о пол"),
            easier = "reverse_lunge", repNote = "всего, на обе ноги", met = 9.0, secPerRep = 2.0,
        ),
        Exercise(
            "glute_bridge", "Ягодичный мостик", Category.LEGS, REPS, "Ягодицы, задняя поверхность бедра",
            listOf("Лёжа на спине, стопы ближе к тазу", "Поднимите таз до прямой линии колени-плечи", "Сожмите ягодицы вверху на секунду"),
            listOf("Прогиб в пояснице вместо работы ягодиц"),
            met = 3.5,
        ),
        Exercise(
            "calf_raise", "Подъёмы на носки", Category.LEGS, REPS, "Икры",
            listOf("Поднимитесь на носки как можно выше", "Задержитесь на секунду наверху", "Медленно опуститесь"),
            listOf("Пружинить без паузы"),
            met = 3.0, secPerRep = 2.0,
        ),

        // ---------- толчок ----------
        Exercise(
            "incline_pushup", "Отжимания от опоры", Category.PUSH, REPS, "Грудь, трицепс, плечи",
            listOf("Ладони на скамье, лежаке или бордюре", "Тело прямое от пяток до головы", "Касайтесь опоры грудью, локти под 45° к телу"),
            listOf("Провисший таз", "Локти разведены в стороны под 90°"),
            equip = Equip.BENCH, harder = "knee_pushup", alt = "knee_pushup", met = 4.0,
        ),
        Exercise(
            "knee_pushup", "Отжимания с колен", Category.PUSH, REPS, "Грудь, трицепс, плечи",
            listOf("Опора на колени и ладони", "Прямая линия от колен до головы", "Грудь почти до пола"),
            listOf("Таз выше линии корпуса"),
            easier = "incline_pushup", harder = "pushup", met = 4.0,
        ),
        Exercise(
            "pushup", "Отжимания", Category.PUSH, REPS, "Грудь, трицепс, плечи, кор",
            listOf("Ладони чуть шире плеч", "Пресс и ягодицы напряжены, тело как доска", "Грудь почти касается пола, полностью выпрямляйте руки"),
            listOf("Голова тянется к полу раньше груди", "Неполная амплитуда"),
            easier = "knee_pushup", harder = "decline_pushup", met = 5.0,
        ),
        Exercise(
            "decline_pushup", "Отжимания с ногами на опоре", Category.PUSH, REPS, "Верх груди, плечи, трицепс",
            listOf("Носки на скамье или бордюре", "Тело прямое, ладони под плечами", "Опускайтесь медленно, 2 секунды"),
            listOf("Проваливать поясницу"),
            equip = Equip.BENCH, easier = "pushup", alt = "pushup", met = 5.5,
        ),
        Exercise(
            "pike_pushup", "Отжимания в складке (пайк)", Category.PUSH, REPS, "Плечи, трицепс",
            listOf("Таз высоко, тело буквой Λ", "Голова опускается к полу перед ладонями", "Локти идут назад, а не в стороны"),
            listOf("Опускать таз и превращать в обычные отжимания"),
            easier = "pushup", met = 5.0,
        ),
        Exercise(
            "bench_dip", "Обратные отжимания от скамьи", Category.PUSH, REPS, "Трицепс, плечи",
            listOf("Ладони на краю скамьи за спиной", "Опускайтесь до угла 90° в локтях", "Спина близко к скамье, плечи опущены"),
            listOf("Опускаться слишком глубоко: болят плечи"),
            equip = Equip.BENCH, alt = "knee_pushup", met = 4.0,
        ),

        // ---------- тяга ----------
        Exercise(
            "dead_hang", "Вис на турнике", Category.PULL, TIME, "Хват, широчайшие, плечи",
            listOf("Хват чуть шире плеч", "Плечи тянем вниз от ушей", "Корпус собран, ноги вместе"),
            listOf("Расслабленные плечи у ушей"),
            equip = Equip.BAR, alt = "superman", harder = "negative_pullup", met = 3.0,
        ),
        Exercise(
            "negative_pullup", "Негативные подтягивания", Category.PULL, REPS, "Спина, бицепс",
            listOf("Запрыгните или встаньте так, чтобы подбородок был над перекладиной", "Опускайтесь медленно, 4–5 секунд", "Внизу полностью выпрямите руки"),
            listOf("Падать вниз за секунду"),
            equip = Equip.BAR, alt = "superman_pull", easier = "dead_hang", harder = "pullup", met = 5.0, secPerRep = 7.0,
        ),
        Exercise(
            "pullup", "Подтягивания", Category.PULL, REPS, "Спина, бицепс",
            listOf("Начинайте из виса с прямыми руками", "Тяните локти вниз, подбородок выше перекладины", "Опускайтесь подконтрольно, без раскачки"),
            listOf("Рывки ногами", "Неполная амплитуда"),
            equip = Equip.BAR, alt = "superman_pull", easier = "negative_pullup", met = 6.0, secPerRep = 4.0,
        ),
        Exercise(
            "australian_row", "Австралийские подтягивания", Category.PULL, REPS, "Спина, бицепс",
            listOf("Низкая перекладина на уровне пояса", "Тело прямое, пятки на земле", "Тяните грудь к перекладине, сводя лопатки"),
            listOf("Провисание таза"),
            equip = Equip.BAR, alt = "superman", harder = "negative_pullup", met = 5.0,
        ),
        Exercise(
            "hanging_knee_raise", "Подъёмы коленей в висе", Category.CORE, REPS, "Пресс, сгибатели бедра",
            listOf("Вис, плечи активны", "Поднимайте колени к груди", "Опускайте медленно, без раскачки"),
            listOf("Раскачиваться и закидывать ноги инерцией"),
            equip = Equip.BAR, alt = "leg_raises", met = 4.5,
        ),
        Exercise(
            "superman", "Супермен", Category.PULL, TIME, "Поясница, ягодицы, верх спины",
            listOf("Лёжа на животе, руки вытянуты вперёд", "Одновременно поднимите руки, грудь и ноги", "Взгляд в пол, шея продолжает позвоночник"),
            listOf("Запрокидывать голову"),
            harder = "superman_pull", met = 3.0,
        ),
        Exercise(
            "superman_pull", "Супермен с тягой", Category.PULL, REPS, "Верх спины, задние дельты",
            listOf("Лёжа на животе, руки и грудь приподняты", "Тяните локти назад к рёбрам, сводя лопатки", "Снова вытяните руки вперёд, не опуская их на пол"),
            listOf("Опускать руки на коврик между повторами"),
            easier = "superman", met = 3.5,
        ),

        // ---------- кор ----------
        Exercise(
            "plank", "Планка", Category.CORE, TIME, "Пресс, кор, плечи",
            listOf("Локти под плечами", "Тело прямое: таз не провисает и не задирается", "Напрягите пресс и ягодицы, дышите ровно"),
            listOf("Провисшая поясница", "Задержка дыхания"),
            met = 3.5,
        ),
        Exercise(
            "side_plank", "Боковая планка", Category.CORE, TIME, "Косые мышцы, кор",
            listOf("Опора на предплечье, локоть под плечом", "Тело в одну линию, таз вверх", "Половину времени на каждую сторону"),
            listOf("Таз провисает к полу"),
            met = 3.5,
        ),
        Exercise(
            "side_plank_l", "Боковая планка: левый бок", Category.CORE, TIME, "Косые мышцы, кор",
            listOf("Лягте на левый бок, опора на левое предплечье", "Локоть строго под плечом, стопы друг на друге", "Таз вверх: тело в одну линию от головы до пяток"),
            listOf("Таз провисает к полу", "Плечо уходит к уху"),
            anim = "side_plank", easier = "side_plank", met = 3.5,
        ),
        Exercise(
            "side_plank_r", "Боковая планка: правый бок", Category.CORE, TIME, "Косые мышцы, кор",
            listOf("Лягте на правый бок, опора на правое предплечье", "Локоть строго под плечом, стопы друг на друге", "Таз вверх: тело в одну линию от головы до пяток"),
            listOf("Таз провисает к полу", "Плечо уходит к уху"),
            anim = "side_plank", easier = "side_plank", met = 3.5,
        ),
        Exercise(
            "high_plank", "Планка на прямых руках", Category.CORE, TIME, "Пресс, плечи, грудь",
            listOf("Ладони под плечами, пальцы вперёд", "Тело прямое, пятки тянутся назад", "Отталкивайте пол, лопатки не проваливаются"),
            listOf("Таз задран вверх", "Провисшие плечи"),
            easier = "plank", met = 3.5,
        ),
        Exercise(
            "bridge_hold", "Удержание мостика", Category.CORE, TIME, "Ягодицы, поясница, задняя поверхность бедра",
            listOf("Лёжа на спине, стопы ближе к тазу", "Поднимите таз до прямой линии колени-плечи", "Держите, сжимая ягодицы, дышите ровно"),
            listOf("Прогиб в пояснице вместо работы ягодиц"),
            easier = "glute_bridge", met = 3.0,
        ),
        Exercise(
            "mountain_climber", "Скалолаз", Category.CORE, TIME, "Кор, плечи, кардио",
            listOf("Упор лёжа, ладони под плечами", "Поочерёдно подтягивайте колени к груди", "Таз на уровне плеч"),
            listOf("Задирать таз вверх"),
            met = 8.0,
        ),
        Exercise(
            "dead_bug", "Мёртвый жук", Category.CORE, REPS, "Глубокие мышцы пресса",
            listOf("Лёжа на спине, руки вверх, колени над тазом", "Опускайте противоположные руку и ногу", "Поясница всё время прижата к полу"),
            listOf("Поясница отрывается от пола"),
            harder = "hollow_hold", repNote = "всего, на обе стороны", met = 3.0, secPerRep = 3.5,
        ),
        Exercise(
            "crunch", "Скручивания", Category.CORE, REPS, "Прямая мышца живота",
            listOf("Лёжа на спине, колени согнуты", "Отрывайте лопатки, подбородок не прижимайте", "Выдох на подъёме"),
            listOf("Тянуть себя за шею"),
            harder = "leg_raises", met = 3.0, secPerRep = 2.5,
        ),
        Exercise(
            "leg_raises", "Подъёмы ног лёжа", Category.CORE, REPS, "Низ пресса",
            listOf("Руки вдоль тела, поясница прижата", "Поднимайте прямые ноги до вертикали", "Опускайте медленно, не касаясь пола"),
            listOf("Прогиб в пояснице при опускании"),
            easier = "crunch", met = 3.5,
        ),
        Exercise(
            "hollow_hold", "Лодочка", Category.CORE, TIME, "Пресс, кор",
            listOf("Лёжа на спине, руки за головой вытянуты", "Плечи и ноги приподняты над полом", "Поясница прижата: если отрывается, поднимите ноги выше"),
            listOf("Прогиб в пояснице"),
            easier = "dead_bug", met = 3.5,
        ),

        // ---------- кардио ----------
        Exercise(
            "high_knees", "Бег с высоким подниманием колен", Category.CARDIO, TIME, "Ноги, кардио",
            listOf("Колени до уровня таза", "Активная работа руками", "Приземляйтесь на переднюю часть стопы"),
            listOf("Заваливаться назад"),
            easier = "march", met = 8.0,
        ),
        Exercise(
            "burpee_low", "Бёрпи без прыжка", Category.CARDIO, REPS, "Всё тело",
            listOf("Присядьте и поставьте ладони на коврик", "Отшагните ногами в планку по одной", "Вернитесь шагами и встаньте"),
            listOf("Провисший таз в планке"),
            harder = "burpee", met = 6.0, secPerRep = 6.0,
        ),
        Exercise(
            "burpee", "Бёрпи", Category.CARDIO, REPS, "Всё тело",
            listOf("Присед, ладони на коврик", "Прыжком в планку и обратно", "Выпрыгните вверх с руками над головой"),
            listOf("Падение грудью на пол без контроля"),
            easier = "burpee_low", met = 9.0, secPerRep = 4.0,
        ),
        Exercise(
            "walk", "Спокойная ходьба", Category.CARDIO, TIME, "Восстановление",
            listOf("Свободный шаг, дыхание через нос", "Плечи расслаблены"),
            listOf(),
            harder = "brisk_walk", anim = "walk", met = 3.0, fixedVolume = true,
        ),
        Exercise(
            "brisk_walk", "Быстрая ходьба по пляжу", Category.CARDIO, TIME, "Сердце, жиросжигание",
            listOf("Темп, при котором можно говорить, но не петь", "Активно работайте руками", "По плотному песку у воды идти легче, по рыхлому тяжелее"),
            listOf("Сутулиться и смотреть под ноги"),
            easier = "walk", harder = "jog", anim = "walk", met = 4.5, fixedVolume = true,
        ),
        Exercise(
            "jog", "Лёгкий бег", Category.CARDIO, TIME, "Сердце, ноги",
            listOf("Короткий шаг, приземление под собой", "Темп разговорный", "Бегайте по плотному песку у кромки воды"),
            listOf("Слишком быстрый старт"),
            easier = "brisk_walk", harder = "sprint", anim = "run", met = 7.5, fixedVolume = true,
        ),
        Exercise(
            "sprint", "Ускорение", Category.CARDIO, TIME, "Сердце, ноги, жиросжигание",
            listOf("Почти максимальный темп", "Высоко колени, активные руки", "Затем полностью восстановитесь шагом"),
            listOf("Бежать первые ускорения в полную силу без разминки"),
            easier = "jog", anim = "run", met = 11.0, fixedVolume = true,
        ),

        // ---------- растяжка ----------
        Exercise(
            "hamstring_stretch", "Наклон к ногам сидя", Category.STRETCH, TIME, "Задняя поверхность бедра, спина",
            listOf("Сидя, ноги прямые вперёд", "Тянитесь грудью к коленям с прямой спиной", "Дышите медленно, без рывков"),
            listOf("Округлять спину и тянуть себя руками"),
            met = 2.3, fixedVolume = true,
        ),
        Exercise(
            "quad_stretch", "Растяжка бедра стоя", Category.STRETCH, TIME, "Передняя поверхность бедра",
            listOf("Возьмите стопу рукой и подтяните к ягодице", "Колени рядом, таз чуть вперёд", "Половину времени на каждую ногу"),
            listOf("Наклоняться вперёд"),
            met = 2.3, fixedVolume = true,
        ),
        Exercise(
            "child_pose", "Поза ребёнка", Category.STRETCH, TIME, "Спина, плечи",
            listOf("Сядьте на пятки, лоб к коврику", "Руки вытянуты вперёд", "Глубоко дышите животом"),
            listOf(),
            met = 2.0, fixedVolume = true,
        ),
        Exercise(
            "cobra", "Кобра", Category.STRETCH, TIME, "Пресс, передняя поверхность тела",
            listOf("Лёжа на животе, ладони под плечами", "Поднимите грудь, таз остаётся на полу", "Плечи опущены, шея длинная"),
            listOf("Боль в пояснице: уменьшите амплитуду"),
            met = 2.0, fixedVolume = true,
        ),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): Exercise = byId[id] ?: error("Нет упражнения $id")
    fun find(id: String): Exercise? = byId[id]

    /** Упражнение с учётом инвентаря: если турника или опоры нет, берём замену. */
    fun available(id: String, s: Settings): Exercise {
        var ex = get(id)
        repeat(3) {
            val missing = (ex.equip == Equip.BAR && !s.hasBar) || (ex.equip == Equip.BENCH && !s.hasBench)
            if (missing && ex.alt != null) ex = get(ex.alt!!)
        }
        return ex
    }

    fun usable(ex: Exercise, s: Settings): Boolean =
        !((ex.equip == Equip.BAR && !s.hasBar) || (ex.equip == Equip.BENCH && !s.hasBench))
}
