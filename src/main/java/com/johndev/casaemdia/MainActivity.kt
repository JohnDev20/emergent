package com.johndev.casaemdia

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import java.math.BigDecimal

class MainActivity : Activity() {
    private val pt = Locale.forLanguageTag("pt-BR")
    private val green = "#164B40"
    private val ink = "#142F29"
    private val paper = "#F6F6EF"
    private val violet = "#594982"
    private val categories = listOf("Mercado", "Moradia", "Água e luz", "Transporte", "Saúde", "Lazer", "Outros")
    private lateinit var page: LinearLayout
    private val prefs by lazy { getSharedPreferences("casa", MODE_PRIVATE) }
    private val expenses by lazy { JSONArray(prefs.getString("expenses", "[]")) }
    private var month = YearMonth.now()
    private var filter = "Todos"
    private var person = ""
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(s: String) = Color.parseColor(s)
    private fun box(fill: String, radius: Int = 16) = GradientDrawable().apply {
        setColor(color(fill)); cornerRadius = dp(radius).toFloat()
    }
    private fun column(padding: Int = 0) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }
    private fun title(s: String, size: Int, tint: String = ink, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size.toFloat(); setTextColor(color(tint))
        if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    }
    private fun action(s: String, fill: String, tint: String = ink, click: () -> Unit) = title(s, 15, tint, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(48); background = box(fill)
        setPadding(dp(9), 0, dp(9), 0); isFocusable = true; setOnClickListener { click() }
    }
    private fun gap(parent: LinearLayout, n: Int) { parent.addView(View(this), 1, dp(n)) }
    private fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
    private fun LinearLayout.flex(v: View) { addView(v, LinearLayout.LayoutParams(0, -2, 1f)) }
    private fun money(c: Long) = NumberFormat.getCurrencyInstance(pt).format(BigDecimal.valueOf(c, 2))
    private fun entries() = (0 until expenses.length()).map { expenses.getJSONObject(it) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = color(green); window.navigationBarColor = color(paper)
        val splash = column().apply { gravity = Gravity.CENTER; setBackgroundColor(color(green)) }
        splash.addView(ImageView(this).apply { setImageResource(R.drawable.marca) }, dp(100), dp(100))
        splash.addView(title("Casa em Dia", 30, "#FFFFFF", true))
        setContentView(splash); splash.postDelayed({ if (!isFinishing && !isDestroyed) home() }, 450)
    }
    private fun home() {
        val root = column(16).apply { setBackgroundColor(color(paper)) }
        val scroll = ScrollView(this); page = column()
        scroll.addView(page); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        gap(root, 8); root.addView(action("＋   Adicionar gasto", green, "#FFFFFF") { form() })
        setContentView(root)
        val brand = row()
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.marca) }, dp(42), dp(42))
        brand.addView(title("  Casa em Dia", 23, green, true)); page.addView(brand); gap(page, 18)
        val nav = row()
        nav.addView(action("‹", "#E5ECE3") { month = month.minusMonths(1); home() }.apply { contentDescription = "Mês anterior"; minWidth = dp(48) })
        nav.flex(title(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", pt)).replaceFirstChar { it.uppercase() }, 19, ink, true).apply { gravity = Gravity.CENTER })
        nav.addView(action("›", "#E5ECE3") { month = month.plusMonths(1); home() }.apply { contentDescription = "Próximo mês"; minWidth = dp(48) })
        page.addView(nav); gap(page, 14)
        val all = entries().filter { it.getString("date").startsWith(month.toString()) }
        val shared = all.filter { it.getString("type") == "Família" }.sumOf { it.getLong("cents") }
        val total = all.sumOf { it.getLong("cents") }
        val card = column(20).apply { background = box(green, 22) }
        card.addView(title("Total do mês", 15, "#D6E8DB")); card.addView(title(money(total), 34, "#FFFFFF", true))
        gap(card, 16)
        card.addView(title("Família   " + money(shared), 16, "#DDF99A", true))
        card.addView(title("Individual   " + money(total - shared), 16, "#E4DCF9", true))
        page.addView(card); gap(page, 22)
        page.addView(title("Lançamentos", 22, ink, true)); gap(page, 8)
        val filters = row()
        for (f in listOf("Todos","Família","Individual")) {
            filters.flex(action(f, if (filter == f) "#DDF99A" else paper) { filter = f; home() })
        }
        page.addView(filters); gap(page, 8)
        val names = entries().map { it.getString("person") }.distinct().sorted()
        page.addView(action(if (person.isEmpty()) "Quem pagou · Todos  ▾" else "Quem pagou · $person  ▾", "#E9E5F1", violet) {
            val choices = listOf("Todas as pessoas") + names
            AlertDialog.Builder(this).setTitle("Quem pagou").setItems(choices.toTypedArray()) { _, i -> person = if(i == 0) "" else choices[i]; home() }.show()
        })
        gap(page, 12)
        val shown = all.filter { (filter == "Todos" || it.getString("type") == filter) && (person.isEmpty() || person == it.getString("person")) }.sortedByDescending { it.getString("date") }
        if (shown.isEmpty()) {
            val empty = column(18).apply { background = box("#FFFFFF") }
            empty.addView(title("Nenhum gasto por aqui", 17, ink, true)); gap(empty, 4)
            empty.addView(title(if (all.isEmpty()) "Registre o primeiro gasto deste mês." else "Experimente outro filtro.", 15))
            page.addView(empty)
        } else {
            page.addView(title("Subtotal  ·  " + money(shown.sumOf { it.getLong("cents") }), 15, green, true)); gap(page, 8)
        }
        for (item in shown) {
            val line = column(14).apply { background = box("#FFFFFF"); isFocusable = true; setOnClickListener { form(item) } }
            val top = row(); top.flex(title(item.getString("name"), 17, ink, true)); top.addView(title(money(item.getLong("cents")), 16, green, true))
            line.addView(top); gap(line, 5)
            val day = LocalDate.parse(item.getString("date")).format(DateTimeFormatter.ofPattern("dd/MM"))
            line.addView(title(listOf(day, item.getString("category"), item.getString("type"), item.getString("person")).joinToString(" · "), 14, violet))
            page.addView(line); gap(page, 7)
        }
    }
    private fun form(item: JSONObject? = null) {
        val body = column(18)
        fun field(label: String, initial: String, hintText: String): EditText {
            body.addView(title(label, 14, green, true))
            return EditText(this).apply { setSingleLine(); hint = hintText; setText(initial); body.addView(this) }
        }
        fun choice(label: String, options: List<String>, key: String): Spinner {
            body.addView(title(label, 14, green, true))
            return Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options)
                setSelection(options.indexOf(item?.optString(key)).coerceAtLeast(0)); body.addView(this, -1, dp(48))
            }
        }
        val name = field("Descrição", item?.optString("name") ?: "", "Ex.: compras da semana")
        val amount = field("Valor em reais", item?.let { BigDecimal.valueOf(it.getLong("cents"),2).toPlainString().replace('.', ',') } ?: "", "0,00")
        amount.inputType = 8194
        val payer = field("Quem pagou", item?.optString("person") ?: person, "Nome da pessoa")
        val type = choice("Tipo de gasto", listOf("Família","Individual"), "type")
        val category = choice("Categoria", categories, "category")
        var date = item?.let { LocalDate.parse(it.getString("date")) } ?: if (month == YearMonth.now()) LocalDate.now() else month.atDay(1)
        fun dateLabel() = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val dateButton = Button(this)
        dateButton.text = dateLabel(); dateButton.setOnClickListener {
            DatePickerDialog(this,{ _,y,m,d -> date = LocalDate.of(y,m+1,d); dateButton.text = dateLabel() },date.year,date.monthValue-1,date.dayOfMonth).show()
        }
        body.addView(title("Data",14,green,true)); body.addView(dateButton)
        val dialog = AlertDialog.Builder(this).setTitle(if(item == null) "Adicionar gasto" else "Editar gasto")
            .setView(ScrollView(this).apply { addView(body) }).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",null)
        if(item != null) dialog.setNeutralButton("Excluir") { _,_ ->
            AlertDialog.Builder(this).setMessage("Excluir este gasto?").setNegativeButton("Cancelar",null).setPositiveButton("Excluir") { _,_ ->
                expenses.remove(entries().indexOf(item)); persist(); home()
            }.show()
        }
        val opened = dialog.show()
        opened.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val description = name.text.toString().trim()
            val who = payer.text.toString().trim()
            val raw = amount.text.toString().trim().replace(',', '.')
            val cents = raw.takeIf { it.matches(Regex("[0-9]{1,9}([.][0-9]{1,2})?")) }?.toBigDecimal()?.movePointRight(2)?.longValueExact()
            when {
                description.isEmpty() -> name.error = "Informe a descrição"
                cents == null || cents <= 0 -> amount.error = "Informe um valor positivo com até 2 casas decimais"
                who.isEmpty() -> payer.error = "Informe quem pagou"
                else -> {
                    val record = item ?: JSONObject()
                    record.put("name",description).put("person",entries().map { it.getString("person") }.firstOrNull { it.equals(who,true) } ?: who)
                        .put("cents",cents).put("type",type.selectedItem.toString()).put("category",category.selectedItem.toString()).put("date",date.toString())
                    if(item == null) expenses.put(record)
                    persist(); month = YearMonth.from(date); filter = "Todos"; person = ""; opened.dismiss(); home()
                }
            }
        }
    }
    private fun persist() { prefs.edit().putString("expenses", expenses.toString()).apply() }
}
