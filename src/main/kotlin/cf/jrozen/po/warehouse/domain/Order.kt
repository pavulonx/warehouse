package cf.jrozen.po.warehouse.domain

import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Order(
        @Id
        val orderId: String
)