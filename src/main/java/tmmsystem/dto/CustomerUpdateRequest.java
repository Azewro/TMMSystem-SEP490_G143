package tmmsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CustomerUpdateRequest {
    @Schema(description = "Tên công ty")
    private String companyName;
    @Schema(description = "Người liên hệ chính")
    private String contactPerson;
    @Schema(description = "Email đăng nhập")
    private String email;
    @Schema(description = "SĐT")
    private String phoneNumber;
    @Schema(description = "Vị trí (Giám đốc, Sales, HR, ...)")
    private String position;
    @Schema(description = "Địa chỉ")
    private String address;
    @Schema(description = "Mã số thuế")
    private String taxCode;
    @Schema(description = "Ngành nghề")
    private String industry;
    @Schema(description = "Loại khách hàng")
    private String customerType;
    @Schema(description = "Hạn mức tín dụng")
    private java.math.BigDecimal creditLimit;
    @Schema(description = "Điều khoản thanh toán")
    private String paymentTerms;
    @Schema(description = "Kích hoạt")
    private Boolean isActive;
    @Schema(description = "Xác minh")
    private Boolean isVerified;
    @Schema(description = "Hình thức đăng ký")
    private String registrationType;
    @Schema(description = "Mật khẩu (tùy chọn)")
    private String password;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public java.math.BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(java.math.BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String registrationType) { this.registrationType = registrationType; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}


