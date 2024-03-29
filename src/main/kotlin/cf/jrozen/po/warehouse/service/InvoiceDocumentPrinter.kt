package cf.jrozen.po.warehouse.service

import cf.jrozen.po.warehouse.domain.Invoice
import cf.jrozen.po.warehouse.domain.Order
import cf.jrozen.po.warehouse.domain.OrderPosition
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * [InvoiceDocumentPrinter] allows to print invoice
 * @property invoice evidence of sale to be printed
 * @property financeProcessingStrategy calculates the prices of goods depending on taxes
 */
class InvoiceDocumentPrinter(
        private val invoice: Invoice,
        financeProcessingStrategy: FinanceProcessingStrategy
) : AbstractDocumentPrinter(financeProcessingStrategy) {

    val font = Font(Font.FontFamily.TIMES_ROMAN, 10f,
            Font.NORMAL, BaseColor.BLACK);

    private var lp: AtomicInteger = resetLp()

    /**
     * Completes the generated invoice with data
     * @return stream of bytes prepared for the data
     */
    override fun printDocument(): ByteArrayOutputStream {
        val baos = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, baos)// FileOutputStream(FILE))

        document.open()
        document.add(Chunk(""))
        addMetadata(document)

        val font = Font(Font.FontFamily.TIMES_ROMAN, 12f,
                Font.NORMAL, BaseColor.BLACK);

        var anchor = Anchor("First Chapter", font)
        anchor.name = "First Chapter"

        // Second parameter is the number of the chapter
        var catPart = Chapter(Paragraph(anchor), 1)

        var subPara = Paragraph("Subcategory 1", font)
        var subCatPart = catPart.addSection(subPara)
        subCatPart.add(Paragraph("Hello"))

        subPara = Paragraph("Subcategory 2", font)
        subCatPart = catPart.addSection(subPara)

        val paragraph = Paragraph()
        subCatPart.add(paragraph)

        // add a table


        // order table
        val orderTable = InvoiceDocumentTemplate.orderTable()
        invoice.order.orderPosition.forEach {
            addOrderPositionToTable(it, orderTable, this::extractOrderPositionProperties)
        }

        // vat table
        val vatTable = InvoiceDocumentTemplate.vatTable()
        fillVatInfo(invoice.order, vatTable)

        subCatPart.add(orderTable)
        subCatPart.add(vatTable)
        document.add(subCatPart)

        document.close()

        resetLp()
        return baos
    }

    private fun addMetadata(document: Document) {
        document.addTitle("INVOICE" + invoice.serialNumber)
        document.addSubject("Invoice")
        document.addKeywords("Sale document, Invoice")
        document.addAuthor("Sezam")
        document.addCreator("Sezam")
    }

    private fun resetLp(): AtomicInteger {
        val atomicInteger = AtomicInteger(1)
        lp = atomicInteger
        return atomicInteger
    }

    private fun fillVatInfo(order: Order, vatTable: PdfPTable) {
        val cells = financeProcessingStrategy.calculatePerTaxGroup(order)

        cells.entries.forEach {
            vatTable.addCell(Phrase(it.key.taxAmount.toString(), font))
            it.value.forEach { bd ->
                vatTable.addCell(Phrase(bd.toString(),font))
            }
        }

        // sum footer
        vatTable.addCell(Phrase(InvoiceDocumentTemplate.SUM_CELL_NAME, font))

        val sum = columnSum(cells.values)//.fold(listOf(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), columnReduceFunction)
        sum.forEach { vatTable.addCell(Phrase(it.toString(),font)) }

    }


    private fun addOrderPositionToTable(
            op: OrderPosition,
            t: PdfPTable,
            propsExtractor: (OrderPosition) -> List<String>) {
        propsExtractor(op).forEach { t.addCell(Phrase(it, font)) }
    }

    private fun extractOrderPositionProperties(op: OrderPosition): List<String> {
        return listOf(
                lp.getAndIncrement().toString(),
                op.ware.name,
                "szt",
                op.amount.toString(),
                op.ware.price.toString(),
                op.ware.taxGroup.taxAmount.toString(),
                financeProcessingStrategy.calculateNetPrice(op).toString(),
                financeProcessingStrategy.calculateVatPrice(op).toString(),
                financeProcessingStrategy.calculateGrossPrice(op).toString()
        )
    }
}

object InvoiceDocumentTemplate {

    val SUM_CELL_NAME = "Suma:"

    private val orderTableHeadersPl = listOf<String>(
            "Lp.",
            "Nazwa towaru",
            "Jm",
            "Ilość",
            "Cena netto",
            "Stawka VAT",
            "Wartość netto",
            "Wartość VAT",
            "Wartość Brutto")

    private val vatTableHeadersPl = listOf<String>(
            "Stawka",
            "Warość netto",
            "Wartość VAT",
            "Wartość brutto")

    private val vatTableLeftColumnPl = listOf<String>(
            "Stawka",
            "Warość netto",
            "Wartość VAT",
            "Wartość brutto")


    /**
     * Passes the order headers to the method that creates the tables in the pdf document.
     * @return calling a function that creates tables with the headers passed in the argument.
     */
    fun orderTable(): PdfPTable {
        return createPdfTable(orderTableHeadersPl)
    }

    /**
     * Passes price type headers to the method that creates the tables in the pdf document.
     * @return calling a function that creates tables with the headers passed in the argument.
     */
    fun vatTable(): PdfPTable {
        return createPdfTable(vatTableHeadersPl)
    }

    private fun createPdfTable(headers: List<String>): PdfPTable {
        val pdfPTable = PdfPTable(headers.size)
        val cells = headers.map { h -> PdfPCell(Phrase(h)) }

        cells.forEach { it.horizontalAlignment = Element.ALIGN_CENTER }
        cells[1].horizontalAlignment = Element.ALIGN_RIGHT
        pdfPTable.headerRows = 1
        return pdfPTable
    }

}