package cf.jrozen.po.warehouse.service

import cf.jrozen.po.warehouse.domain.DocumentState
import cf.jrozen.po.warehouse.domain.Order
import cf.jrozen.po.warehouse.domain.SaleDocumentType
import cf.jrozen.po.warehouse.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = false)
class OrderService(
        val orderRepository: OrderRepository,
        val saleDocumentService: SaleDocumentService) {

    @Transactional(readOnly = true)
    fun getAllOrders(): MutableList<Order> = orderRepository.findAll()

    @Transactional(readOnly = true)
    fun getOrder(orderId: String): Order = orderRepository.getOne(orderId)

    fun updateOrder(order: Order): Order = orderRepository.save(order)

    fun createSaleDocument(orderId: String, saleDocumentRequest: SaleDocumentRequest): Order {
        val order: Order = orderRepository.getOne(orderId)
        if (!order.canGenerateSaleDocument())
            throw IllegalArgumentException("Cannot generate sale document for order: $orderId")
        if (saleDocumentRequest.type == SaleDocumentType.INVOICE
                && order.customer.nip == null)
            throw IllegalArgumentException("cannot generate sale document for order: $order, cause nip is not present")

        saleDocumentService.generateNewSaleDocument(order, saleDocumentRequest)

        order.documentState = DocumentState.REALIZED
        orderRepository.save(order)
        return order
    }

}