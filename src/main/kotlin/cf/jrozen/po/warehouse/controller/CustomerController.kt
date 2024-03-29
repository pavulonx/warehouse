package cf.jrozen.po.warehouse.controller

import cf.jrozen.po.warehouse.common.ErrorKeys
import cf.jrozen.po.warehouse.common.ErrorKeys.NULL_CUSTOMERS_UUID
import cf.jrozen.po.warehouse.common.RequestValidationException
import cf.jrozen.po.warehouse.controller.dto.CustomerDto
import cf.jrozen.po.warehouse.domain.Customer
import cf.jrozen.po.warehouse.service.CustomerService
import cf.jrozen.po.warehouse.utils.randomUUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder

/**
 * [CustomerController] is responsible for managing with [Customer] entity
 * @property customerService enables customer service
 * @property customerValidator allows customer validation
 */
@RestController
@RequestMapping("/customers")
class CustomerController(
        val customerService: CustomerService,
        val customerValidator: CustomerValidator
) {

    /**
     * Goes to database for all [Customer]s in database
     * @return all [Customer]s in repository in JSON format
     */
    @GetMapping
    fun getAllCustomers(): List<Customer> = customerService.getAllCustomers()

    /**
     * Goes to database for [Customer] with a specific identifier [customerUuid] in database
     * @param [customerUuid] is specific identifier
     * @return [Customer] in repository in JSON format
     */
    @GetMapping("/{customerUuid}")
    fun getCustomer(@PathVariable customerUuid: String): Customer =
            customerService.getCustomer(customerUuid)

    /**
     * Goes to database for specific [Customer] in database
     * @param [customer] where the change is made
     * @return upgraded [Customer] in JSON format
     */
    @PutMapping
    fun updateCustomer(@RequestBody @Validated customer: CustomerDto): Customer {
        customer.customerUuid ?:
                throw RequestValidationException("Customer Uuid is null", NULL_CUSTOMERS_UUID)
        return customerService.updateCustomer(customer)
    }

    /**
     * Saves [Customer] to repository
     * @param [customer] which will be saved in the database
     * @return saved [Customer] in JSON format
     */
    @PostMapping
    fun saveCustomer(@RequestBody @Validated customer: CustomerDto): Customer {
        return customerService.saveCustomer(customer)
    }

    /**
     * Deletes [Customer] with a specific identifier [customerUuid] from the repository
     * @param [customerUuid] is specific identifier
     * @return deleted [Customer] in JSON format
     */
    @DeleteMapping("/{customerUuid}")
    fun deleteCustomer(@PathVariable customerUuid: String): Customer =
            customerService.deleteCustomer(customerUuid)

    @InitBinder("customer")
    fun addCustomerValidator(binder: WebDataBinder) {
        binder.validator = customerValidator
    }
}