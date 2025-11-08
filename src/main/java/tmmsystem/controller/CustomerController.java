package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.CustomerDto;
import tmmsystem.dto.CustomerCreateRequest;
import tmmsystem.dto.CustomerUpdateRequest;
import tmmsystem.entity.Customer;
import tmmsystem.mapper.CustomerMapper;
import tmmsystem.service.CustomerService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/customers")
@Validated
public class CustomerController {
    private final CustomerService service;
    private final CustomerMapper mapper;
    public CustomerController(CustomerService service, CustomerMapper mapper) { this.service = service; this.mapper = mapper; }

    @GetMapping
    public List<CustomerDto> list() { return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Long id) { return mapper.toDto(service.findById(id)); }

    @Operation(summary = "Tạo khách hàng")
    @PostMapping
    public CustomerDto create(
            @RequestBody(description = "Payload tạo khách hàng", required = true,
                    content = @Content(schema = @Schema(implementation = CustomerCreateRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setCompanyName(request.companyName());
        customer.setContactPerson(request.contactPerson());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setPosition(request.position());
        customer.setAddress(request.address());
        customer.setTaxCode(request.taxCode());
        customer.setActive(request.isActive() != null ? request.isActive() : true);
        customer.setVerified(request.isVerified() != null ? request.isVerified() : false);
        customer.setRegistrationType(request.registrationType() != null ? request.registrationType() : "SALES_CREATED");
        Customer created = service.create(customer, request.createdById());
        return mapper.toDto(created);
    }

    @Operation(summary = "Cập nhật khách hàng")
    @PutMapping("/{id}")
    public CustomerDto update(
            @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật khách hàng", required = true,
                    content = @Content(schema = @Schema(implementation = CustomerUpdateRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody CustomerUpdateRequest body) {
        Customer updated = new Customer();
        updated.setCompanyName(body.getCompanyName());
        updated.setContactPerson(body.getContactPerson());
        updated.setEmail(body.getEmail());
        updated.setPhoneNumber(body.getPhoneNumber());
        updated.setPosition(body.getPosition());
        updated.setAddress(body.getAddress());
        updated.setTaxCode(body.getTaxCode());
        updated.setIndustry(body.getIndustry());
        updated.setCustomerType(body.getCustomerType());
        updated.setCreditLimit(body.getCreditLimit());
        updated.setPaymentTerms(body.getPaymentTerms());
        updated.setActive(body.getIsActive());
        updated.setVerified(body.getIsVerified());
        updated.setRegistrationType(body.getRegistrationType());
        Customer res = service.update(id, updated);
        return mapper.toDto(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<?> setActive(@PathVariable Long id, @RequestParam boolean value) {
        service.setActive(id, value);
        return ResponseEntity.ok().build();
    }
}


