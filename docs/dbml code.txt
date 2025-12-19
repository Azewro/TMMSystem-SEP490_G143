// ============================================================================
// TMMS - Towel Manufacturing Management System
// Database Schema - Based on Current Backend Entities
// Generated from Java JPA Entities
// ============================================================================

Project TMMS {
  database_type: 'MySQL'
  Note: '''
    Towel Manufacturing Management System
    Database schema based on actual backend entities
  '''
}

// ============================================================================
// LAYER 0: FOUNDATION - Master Data
// ============================================================================

Table role {
  id bigint [pk, increment]
  name varchar(50) [not null, unique]
  description text
  is_active boolean [default: true]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 0 - Foundation: User roles for RBAC
    Valid values: ADMIN, SALES, PLANNING, DIRECTOR, TECHNICAL, PRODUCTION, QA, WAREHOUSE
  '''
}

Table user {
  id bigint [pk, increment]
  employee_code varchar(30) [not null, unique]
  email varchar(150) [not null, unique]
  password varchar(255) [not null]
  name varchar(150) [not null]
  phone_number varchar(30)
  avatar varchar(255)
  is_active boolean [default: true]
  is_verified boolean [default: false]
  reset_code varchar(12)
  reset_code_expires_at timestamp
  last_login_at timestamp
  deleted_at timestamp
  role_id bigint [not null, ref: > role.id]
  created_by bigint [ref: > user.id]
  updated_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 0 - Foundation: Internal system users (employees)
    Employee code format: EMP-YYYYMM-XXX
    Supports soft delete via deleted_at
  '''
}

Table customer {
  id bigint [pk, increment]
  customer_code varchar(30) [not null, unique]
  company_name varchar(255)
  tax_code varchar(50)
  business_license varchar(100)
  address text
  contact_person varchar(150)
  email varchar(150) [not null, unique]
  phone_number varchar(30) [unique]
  position varchar(100)
  is_verified boolean [default: false]
  last_login_at timestamp
  password varchar(255)
  force_password_change boolean [default: false]
  additional_contacts json
  customer_type varchar(20) [default: 'B2B']
  industry varchar(100)
  credit_limit decimal(15,2) [default: 0]
  payment_terms varchar(100)
  is_active boolean [default: true]
  registration_type varchar(100) [default: 'SALES_CREATED']
  sales_in_charge_id bigint [ref: > user.id]
  created_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 0 - Foundation: Customer management (B2B and B2C)
    Customer code format: CUS-YYYYMM-XXX
    Supports password-based login with force password change on first login
    Registration types: SALES_CREATED, SELF_REGISTERED, IMPORTED
  '''
}

Table otp_token {
  id bigint [pk, increment]
  customer_id bigint [not null, ref: > customer.id]
  otp_code varchar(6) [not null]
  expired_at timestamp [not null]
  is_used boolean [not null, default: false]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 0 - Foundation: OTP tokens for customer authentication
    Used for email/SMS verification and password reset
  '''
}

Table product_category {
  id bigint [pk, increment]
  name varchar(100) [not null]
  description text
  parent_id bigint [ref: > product_category.id]
  display_order int [default: 0]
  is_active boolean [default: true]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 0 - Foundation: Product classification hierarchy
    Examples: Bath Towels, Face Towels, Kitchen Towels, Beach Towels
    Supports hierarchical structure via parent_id
  '''
}

Table machine {
  id bigint [pk, increment]
  code varchar(50) [not null, unique]
  name varchar(255) [not null]
  type varchar(20) [not null]
  status varchar(20) [default: 'AVAILABLE']
  location varchar(100)
  specifications json
  last_maintenance_at timestamp
  next_maintenance_at timestamp
  maintenance_interval_days int [default: 90]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 0 - Foundation: Production machines inventory
    Machine types: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING
    Status values: AVAILABLE, IN_USE, MAINTENANCE, BROKEN
  '''
}

// ============================================================================
// LAYER 1: SALES FLOW
// ============================================================================

Table rfq {
  id bigint [pk, increment]
  rfq_number varchar(50) [not null, unique]
  customer_id bigint [not null, ref: > customer.id]
  source_type varchar(30) [default: 'CUSTOMER_PORTAL']
  expected_delivery_date date
  status varchar(100) [default: 'DRAFT']
  is_sent boolean [default: false]
  notes text
  created_by bigint [ref: > user.id]
  assigned_sales_id bigint [ref: > user.id]
  assigned_planning_id bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  approval_date timestamp
  sales_confirmed_at timestamp
  sales_confirmed_by bigint [ref: > user.id]
  is_locked boolean [default: false]
  capacity_status varchar(20)
  capacity_reason text
  proposed_new_delivery_date date
  contact_person_snapshot varchar(150)
  contact_email_snapshot varchar(150)
  contact_phone_snapshot varchar(30)
  contact_address_snapshot text
  contact_method varchar(10)
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 1 - Sales: Request for Quotation (Customer inquiry)
    RFQ number format: RFQ-YYYYMMDD-XXX
    Source types: CUSTOMER_PORTAL, PUBLIC_FORM, BY_SALES
    Status values: DRAFT, SUBMITTED, APPROVED, REJECTED, QUOTED, CANCELED
    Contact snapshots preserve customer info at time of RFQ creation
  '''
}

Table rfq_detail {
  id bigint [pk, increment]
  rfq_id bigint [not null, ref: > rfq.id]
  product_id bigint [not null, ref: > product.id]
  quantity decimal(10,2) [not null]
  unit varchar(20) [default: 'UNIT']
  note_color varchar(100)
  notes text
  
  Note: '''
    Layer 1 - Sales: RFQ line items (products requested by customer)
    Each detail represents one product in the RFQ
  '''
}

Table quotation {
  id bigint [pk, increment]
  quotation_number varchar(50) [not null, unique]
  rfq_id bigint [ref: > rfq.id]
  customer_id bigint [not null, ref: > customer.id]
  valid_until date [not null]
  total_amount decimal(15,2) [not null]
  file_path varchar(500)
  status varchar(20) [default: 'DRAFT']
  sent_at timestamp
  is_accepted boolean [default: false]
  accepted_at timestamp
  is_canceled boolean [default: false]
  rejected_at timestamp
  reject_reason text
  capacity_checked_by bigint [ref: > user.id]
  capacity_checked_at timestamp
  capacity_check_notes text
  assigned_sales_id bigint [ref: > user.id]
  assigned_planning_id bigint [ref: > user.id]
  created_by bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  contact_person_snapshot varchar(150)
  contact_email_snapshot varchar(150)
  contact_phone_snapshot varchar(30)
  contact_address_snapshot text
  contact_method varchar(10)
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 1 - Sales: Sales response with pricing
    Quotation number format: QUO-YYYYMMDD-XXX
    Status values: DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CANCELED
    Includes capacity check workflow (Planning Department)
  '''
}

Table quotation_detail {
  id bigint [pk, increment]
  quotation_id bigint [not null, ref: > quotation.id]
  product_id bigint [not null, ref: > product.id]
  quantity decimal(10,2) [not null]
  unit varchar(20) [default: 'UNIT']
  unit_price decimal(12,2) [not null]
  total_price decimal(15,2) [not null]
  note_color varchar(100)
  discount_percentage decimal(5,2) [default: 0]
  
  Note: '''
    Layer 1 - Sales: Pricing breakdown per product
    total_price = quantity * unit_price * (1 - discount_percentage / 100)
  '''
}

Table contract {
  id bigint [pk, increment]
  contract_number varchar(50) [not null, unique]
  quotation_id bigint [ref: > quotation.id]
  customer_id bigint [not null, ref: > customer.id]
  contract_date date [not null]
  delivery_date date [not null]
  total_amount decimal(15,2) [not null]
  file_path varchar(500)
  status varchar(20) [default: 'DRAFT']
  assigned_sales_id bigint [ref: > user.id]
  assigned_planning_id bigint [ref: > user.id]
  director_approved_by bigint [ref: > user.id]
  director_approved_at timestamp
  director_approval_notes text
  sales_approved_by bigint [ref: > user.id]
  sales_approved_at timestamp
  planning_approved_by bigint [ref: > user.id]
  planning_approved_at timestamp
  created_by bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 1 - Sales: Signed legal agreement → TRIGGERS PRODUCTION
    Contract number format: CON-YYYYMMDD-XXX
    Status values: DRAFT, PENDING_APPROVAL, APPROVED, SIGNED, CANCELED
    Workflow: Contract created → Director/Sales/Planning approves → Customer signs → Production starts
  '''
}

// ============================================================================
// LAYER 1.5: PAYMENT TRACKING
// ============================================================================

Table payment_term {
  id bigint [pk, increment]
  contract_id bigint [not null, ref: > contract.id]
  term_sequence int [not null]
  term_name varchar(100) [not null]
  percentage decimal(5,2) [not null]
  amount decimal(15,2) [not null]
  due_date date
  description text
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 1.5 - Payment: Payment terms definition
    Example: Contract $10,000
      - Term 1: 30% ($3,000) deposit upon signing
      - Term 2: 50% ($5,000) after production complete
      - Term 3: 20% ($2,000) after delivery
  '''
}

Table payment {
  id bigint [pk, increment]
  contract_id bigint [not null, ref: > contract.id]
  payment_term_id bigint [ref: > payment_term.id]
  payment_type varchar(20) [not null]
  amount decimal(15,2) [not null]
  payment_date date [not null]
  payment_method varchar(50) [not null]
  payment_reference varchar(100)
  status varchar(20) [default: 'PENDING']
  invoice_number varchar(50)
  receipt_file_path varchar(500)
  notes text
  created_by bigint [ref: > user.id]
  verified_by bigint [ref: > user.id]
  verified_at timestamp
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 1.5 - Payment: Payment tracking (audit trail)
    Payment types: DEPOSIT, MILESTONE, FINAL, EXTRA
    Payment methods: BANK_TRANSFER, CASH, CHECK, CREDIT_CARD
    Status values: PENDING, COMPLETED, FAILED, REFUNDED
    Links: contract → payment_term → payment
  '''
}

// ============================================================================
// LAYER 2: PRODUCTION PLANNING
// ============================================================================

Table production_lot {
  id bigint [pk, increment]
  lot_code varchar(50) [not null, unique]
  product_id bigint [ref: > product.id]
  size_snapshot varchar(200)
  total_quantity decimal(12,2)
  delivery_date_target date
  contract_date_min date
  contract_date_max date
  status varchar(30) [default: 'FORMING']
  material_requirements_json json
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 2 - Planning: Production lot grouping
    Status values: FORMING, READY_FOR_PLANNING, PLANNING, PLAN_APPROVED, IN_PRODUCTION, COMPLETED, CANCELED
    Groups multiple contracts/orders for batch production
  '''
}

Table production_lot_order {
  id bigint [pk, increment]
  lot_id bigint [ref: > production_lot.id]
  contract_id bigint [ref: > contract.id]
  quotation_detail_id bigint [ref: > quotation_detail.id]
  allocated_quantity decimal(12,2)
  
  Note: '''
    Layer 2 - Planning: Links contracts/orders to production lots
    Junction table for lot grouping functionality
  '''
}

Table production_plan {
  id bigint [pk, increment]
  contract_id bigint [not null, ref: > contract.id]
  plan_code varchar(50) [not null, unique]
  status varchar(20) [not null, default: 'DRAFT']
  created_by bigint [not null, ref: > user.id]
  approved_by bigint [ref: > user.id]
  approved_at timestamp
  approval_notes text
  lot_id bigint [ref: > production_lot.id]
  version_no int [default: 1]
  is_current_version boolean [default: true]
  proposed_start_date date
  proposed_end_date date
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 2 - Planning: Production plan summary
    Plan code format: PP-2025-001
    Status values: DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, SUPERSEDED
    Supports versioning for plan revisions
    When Director approves → system auto-generates Production Order
  '''
}

Table production_plan_stage {
  id bigint [pk, increment]
  plan_id bigint [not null, ref: > production_plan.id]
  stage_type varchar(20) [not null]
  sequence_no int [not null]
  assigned_machine_id bigint [ref: > machine.id]
  in_charge_user_id bigint [ref: > user.id]
  qc_user_id bigint [ref: > user.id]
  planned_start_time datetime [not null]
  planned_end_time datetime [not null]
  min_required_duration_minutes int
  transfer_batch_quantity decimal(10,2)
  capacity_per_hour decimal(10,2)
  stage_status varchar(20) [default: 'PENDING']
  setup_time_minutes int
  teardown_time_minutes int
  actual_start_time datetime
  actual_end_time datetime
  downtime_minutes int
  downtime_reason varchar(200)
  quantity_input decimal(12,2)
  quantity_output decimal(12,2)
  notes text
  
  Note: '''
    Layer 2 - Planning: Stage details for production plan
    Stage types: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING
    Stage status: PENDING, READY, IN_PROGRESS, PAUSED, COMPLETED, CANCELED
    After approval, generates Work Order with corresponding stages
  '''
}

Table production_order {
  id bigint [pk, increment]
  po_number varchar(50) [not null, unique]
  contract_id bigint [ref: > contract.id]
  contract_ids json
  total_quantity decimal(10,2) [not null]
  planned_start_date date
  planned_end_date date
  status varchar(30) [default: 'DRAFT']
  priority int [default: 0]
  notes text
  created_by bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  approved_at timestamp
  assigned_technician_id bigint [ref: > user.id]
  assigned_at timestamp
  execution_status varchar(40)
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 2 - Planning: Top-level production planning
    PO number format: PO-YYYYMMDD-XXX
    Status values: DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELED
    Execution status: WAITING_PRODUCTION, IN_PROGRESS, WAITING_MATERIAL_APPROVAL, WAITING_REWORK, IN_REWORK, COMPLETED
    Can link to multiple contracts via contract_ids JSON array
  '''
}

Table production_order_detail {
  id bigint [pk, increment]
  production_order_id bigint [not null, ref: > production_order.id]
  product_id bigint [not null, ref: > product.id]
  bom_id bigint [not null, ref: > bom.id]
  bom_version varchar(20)
  quantity decimal(10,2) [not null]
  unit varchar(20) [default: 'UNIT']
  note_color varchar(100)
  
  Note: '''
    Layer 2 - Planning: Products to manufacture in this PO
    Locks BOM version at production time (prevents mid-production BOM changes)
    bom_version stores snapshot for audit trail
  '''
}

Table technical_sheet {
  id bigint [pk, increment]
  production_order_id bigint [not null, ref: > production_order.id]
  sheet_number varchar(50) [not null, unique]
  yarn_specifications json
  machine_settings json
  quality_standards json
  special_instructions text
  created_by bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 2 - Planning: Technical specifications
    Sheet number format: TECH-YYYYMMDD-XXX
    Created by Technical Department after PO approval
    Contains yarn specs, machine settings, quality standards per stage
  '''
}

Table work_order {
  id bigint [pk, increment]
  wo_number varchar(50) [not null, unique]
  production_order_id bigint [not null, ref: > production_order.id]
  deadline date
  status varchar(30) [default: 'DRAFT']
  send_status varchar(20) [default: 'NOT_SENT']
  is_production boolean [default: true]
  created_by bigint [ref: > user.id]
  approved_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 2 - Planning: PO split by machine capacity & scheduling
    WO number format: WO-YYYYMMDD-XXX
    Status values: DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELED
    Send status: NOT_SENT, SENT_TO_FLOOR
    One PO can generate multiple WOs based on capacity
  '''
}

Table work_order_detail {
  id bigint [pk, increment]
  work_order_id bigint [not null, ref: > work_order.id]
  production_order_detail_id bigint [not null, ref: > production_order_detail.id]
  stage_sequence int
  planned_start_at timestamp
  planned_end_at timestamp
  start_at timestamp
  complete_at timestamp
  work_status varchar(20) [default: 'PENDING']
  notes text
  
  Note: '''
    Layer 2 - Planning: Each WO detail creates production stages
    Work status values: PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELED
    Links WO to specific product from PO detail
  '''
}

Table production_stage {
  id bigint [pk, increment]
  production_order_id bigint [ref: > production_order.id]
  stage_type varchar(20) [not null]
  stage_sequence int [not null]
  machine_id bigint [ref: > machine.id]
  assigned_to bigint [ref: > user.id]
  assigned_leader_id bigint [ref: > user.id]
  batch_number varchar(50)
  planned_output decimal(10,2)
  actual_output decimal(10,2)
  start_at timestamp
  complete_at timestamp
  status varchar(20) [default: 'PENDING']
  is_outsourced boolean [default: false]
  outsource_vendor varchar(255)
  notes text
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  planned_start_at timestamp
  planned_end_at timestamp
  planned_duration_hours decimal(10,2)
  qr_token varchar(64) [unique]
  qc_last_result varchar(20)
  qc_last_checked_at timestamp
  qc_assignee_id bigint [ref: > user.id]
  execution_status varchar(30)
  progress_percent int
  is_rework boolean [default: false]
  original_stage_id bigint [ref: > production_stage.id]
  
  Note: '''
    Layer 2 - CENTRAL HUB: Each manufacturing step
    Stage types: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING
    Status values: PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELED
    Execution status: WAITING, IN_PROGRESS, WAITING_QC, QC_IN_PROGRESS, QC_PASSED, QC_FAILED, WAITING_REWORK, REWORK_IN_PROGRESS, COMPLETED
    Connects to 10+ tables (tracking, QC, materials, machines)
    Supports rework workflow via is_rework and original_stage_id
  '''
}

// ============================================================================
// LAYER 3: PRODUCTION EXECUTION
// ============================================================================

Table stage_tracking {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  operator_id bigint [not null, ref: > user.id]
  action varchar(20) [not null]
  quantity_completed decimal(10,2)
  notes text
  timestamp timestamp [not null, default: `CURRENT_TIMESTAMP`]
  evidence_photo_url varchar(500)
  is_rework boolean [default: false]
  
  Note: '''
    Layer 3 - Execution: Real-time event log (like Git commits)
    Action values: START, PAUSE, RESUME, COMPLETE, REPORT_ISSUE
    Every stage action is logged here for audit trail
    Supports rework tracking
  '''
}

Table stage_pause_log {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  paused_by bigint [not null, ref: > user.id]
  resumed_by bigint [ref: > user.id]
  pause_reason varchar(30) [not null]
  pause_notes text
  paused_at timestamp [not null]
  resumed_at timestamp
  duration_minutes int
  
  Note: '''
    Layer 3 - Execution: Track WHY production stopped
    Pause reasons: MACHINE_BREAKDOWN, MATERIAL_SHORTAGE, SHIFT_END, QUALITY_ISSUE
    Used for downtime analysis and OEE calculation
  '''
}

Table outsourcing_task {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  vendor_name varchar(255) [not null]
  delivery_note_number varchar(50)
  weight_sent decimal(10,3)
  weight_returned decimal(10,3)
  shrinkage_rate decimal(5,2)
  expected_quantity decimal(10,2)
  returned_quantity decimal(10,2)
  unit_cost decimal(12,2)
  total_cost decimal(15,2)
  sent_at timestamp
  expected_return_date date
  actual_return_date date
  status varchar(20) [default: 'SENT']
  notes text
  created_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 3 - Execution: Track stages sent to external vendors
    Status values: SENT, IN_PROGRESS, RETURNED, QUALITY_ISSUE
    Typical for DYEING stage (Vendor Dyeing)
    Tracks weight, shrinkage, and cost for vendor management
  '''
}

Table production_loss {
  id bigint [pk, increment]
  production_order_id bigint [not null, ref: > production_order.id]
  material_id bigint [not null, ref: > material.id]
  quantity_lost decimal(10,3) [not null]
  loss_type varchar(20) [not null]
  production_stage_id bigint [ref: > production_stage.id]
  notes text
  recorded_by bigint [not null, ref: > user.id]
  recorded_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 3 - Execution: Track material waste for cost analysis
    Loss types: CANCELLATION, DEFECT, WASTE, SHRINKAGE
    Links to production stage to identify where loss occurred
  '''
}

Table material_requisition {
  id bigint [pk, increment]
  requisition_number varchar(50) [not null, unique]
  production_stage_id bigint [not null, ref: > production_stage.id]
  requested_by bigint [not null, ref: > user.id]
  approved_by bigint [ref: > user.id]
  status varchar(20) [default: 'PENDING']
  requested_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  approved_at timestamp
  issued_at timestamp
  notes text
  source_issue_id bigint [ref: > quality_issue.id]
  quantity_requested decimal(10,2)
  quantity_approved decimal(10,2)
  requisition_type varchar(30)
  
  Note: '''
    Layer 3 - Execution: Material request workflow
    Requisition number format: REQ-YYYYMMDD-XXX
    Status values: PENDING, APPROVED, ISSUED, REJECTED, CANCELED
    Requisition types: YARN_SUPPLY, OTHER
    Workflow: Leader requests → Warehouse approves → Materials issued
    Can be triggered by quality issues (source_issue_id)
  '''
}

Table material_requisition_detail {
  id bigint [pk, increment]
  requisition_id bigint [not null, ref: > material_requisition.id]
  material_id bigint [not null, ref: > material.id]
  quantity_requested decimal(10,3) [not null]
  quantity_approved decimal(10,3)
  quantity_issued decimal(10,3)
  unit varchar(20) [default: 'KG']
  notes text
  
  Note: '''
    Layer 3 - Execution: Material requisition line items
    Tracks requested, approved, and issued quantities separately
  '''
}

// ============================================================================
// LAYER 4: QUALITY CONTROL
// ============================================================================

Table qc_checkpoint {
  id bigint [pk, increment]
  stage_type varchar(20) [not null]
  checkpoint_name varchar(255) [not null]
  inspection_criteria text
  sampling_plan text
  is_mandatory boolean [default: true]
  display_order int [default: 0]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 4 - Quality: Define WHAT to inspect at each stage
    Stage types match workflow: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING
    Master data - seed at deployment
    Each checkpoint defines inspection criteria and sampling plan
  '''
}

Table qc_session {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  started_by_id bigint [not null, ref: > user.id]
  status varchar(20) [default: 'IN_PROGRESS']
  overall_result varchar(20)
  notes text
  started_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  submitted_at timestamp
  
  Note: '''
    Layer 4 - Quality: QC inspection session container
    Status values: IN_PROGRESS, SUBMITTED
    Overall result: PASS, FAIL
    Groups multiple qc_inspection records for a single QC session
  '''
}

Table qc_inspection {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  qc_checkpoint_id bigint [not null, ref: > qc_checkpoint.id]
  inspector_id bigint [not null, ref: > user.id]
  sample_size int
  pass_count int
  fail_count int
  result varchar(20) [not null]
  notes text
  photo_url text
  inspected_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 4 - Quality: Record inspection results
    Result values: PASS, FAIL, CONDITIONAL_PASS
    ISO 9001 compliance tracking
    Links to checkpoint for inspection criteria reference
  '''
}

Table qc_defect {
  id bigint [pk, increment]
  qc_inspection_id bigint [not null, ref: > qc_inspection.id]
  defect_type varchar(30) [not null]
  defect_description text
  quantity_affected decimal(10,2)
  severity varchar(20) [not null]
  action_taken varchar(20)
  
  Note: '''
    Layer 4 - Quality: Log specific defects found during inspection
    Defect types: COLOR_DEVIATION, WEIGHT_ISSUE, DIMENSION_ERROR, SURFACE_DEFECT
    Severity: MINOR (cosmetic), MAJOR (functional), CRITICAL (safety)
    Action taken: REWORK, SCRAP, DOWNGRADE, APPROVED_WITH_DEVIATION
  '''
}

Table qc_photo {
  id bigint [pk, increment]
  qc_inspection_id bigint [not null, ref: > qc_inspection.id]
  photo_url varchar(500) [not null]
  caption varchar(255)
  uploaded_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 4 - Quality: Photo evidence of defects or quality issues
    Links to S3/Firebase/Cloudinary storage
  '''
}

Table qc_standard {
  id bigint [pk, increment]
  standard_name varchar(255) [not null]
  standard_code varchar(50)
  description text
  applicable_stages varchar(255)
  is_active boolean [default: true]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 4 - Quality: Reference standards (ISO 9001, industry standards)
    Standard codes: ISO 9001, Oeko-Tex 100, ASTM D1776
    Applicable stages: Comma-separated list of stage types
  '''
}

Table quality_issue {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  production_order_id bigint [ref: > production_order.id]
  severity varchar(20)
  issue_type varchar(30)
  status varchar(20) [default: 'PENDING']
  description text
  created_by_id bigint [ref: > user.id]
  processed_by_id bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  processed_at timestamp
  material_needed boolean [default: false]
  evidence_photo varchar(500)
  
  Note: '''
    Layer 4 - Quality: Quality issues requiring action
    Severity: MINOR, MAJOR
    Issue types: REWORK, MATERIAL_REQUEST
    Status: PENDING, PROCESSED
    When material_needed=true, can trigger material_requisition
  '''
}

Table stage_risk_assessment {
  id bigint [pk, increment]
  production_stage_id bigint [not null, ref: > production_stage.id]
  severity varchar(10) [not null]
  description text
  root_cause text
  solution_proposal text
  status varchar(20) [not null, default: 'OPEN']
  approved_by bigint [ref: > user.id]
  approved_at timestamp
  impacted_delivery boolean
  proposed_new_date date
  notes text
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 4 - Quality: Risk assessment for production stages
    Severity: MINOR, MAJOR
    Status: OPEN, IN_REVIEW, APPROVED, REJECTED, CLOSED
    Tracks delivery impact and proposed solutions
  '''
}

Table stage_risk_attachment {
  id bigint [pk, increment]
  risk_assessment_id bigint [not null, ref: > stage_risk_assessment.id]
  file_url varchar(500) [not null]
  caption varchar(255)
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 4 - Quality: Supporting documents for risk assessments
    Stores file URLs for evidence, photos, reports
  '''
}

// ============================================================================
// LAYER 5: PRODUCTS & MATERIALS
// ============================================================================

Table product {
  id bigint [pk, increment]
  code varchar(50) [not null, unique]
  name varchar(255) [not null]
  description text
  category_id bigint [ref: > product_category.id]
  unit varchar(20) [default: 'UNIT']
  standard_weight decimal(10,3)
  standard_dimensions varchar(100)
  base_price decimal(12,2)
  is_active boolean [default: true]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 5 - Product: Finished goods catalog
    Product code format: TWL-30x50-WHT (example)
    Standard weight in kg per unit
    Standard dimensions: e.g., 30cm x 50cm
  '''
}

Table material {
  id bigint [pk, increment]
  code varchar(50) [not null, unique]
  name varchar(255) [not null]
  type varchar(20) [not null]
  unit varchar(20) [default: 'KG']
  reorder_point decimal(10,3)
  standard_cost decimal(12,2)
  is_active boolean [default: true]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 5 - Product: Raw materials and consumables
    Material types: RAW_COTTON, YARN, DYE, CHEMICAL, PACKAGING
    Material code format: YARN-20S-WHT, DYE-BLUE-001 (examples)
    Reorder point triggers inventory alerts
  '''
}

Table bom {
  id bigint [pk, increment]
  product_id bigint [not null, ref: > product.id]
  version varchar(20) [not null]
  version_notes text
  is_active boolean [default: true]
  effective_date date
  obsolete_date date
  created_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 5 - Product: Bill of Materials - recipe for each product
    Version control: Track BOM changes over time (v1.0, v1.1, v2.0)
    Only one active BOM per product (enforced in service layer)
    Created by Technical Department
  '''
}

Table bom_detail {
  id bigint [pk, increment]
  bom_id bigint [not null, ref: > bom.id]
  material_id bigint [not null, ref: > material.id]
  quantity decimal(10,3) [not null]
  unit varchar(20) [default: 'KG']
  stage varchar(20)
  is_optional boolean [default: false]
  notes text
  
  Note: '''
    Layer 5 - Product: Materials needed per product
    Example: To make 1 bath towel (30x50cm, 100g):
      - 250g yarn (accounting for waste)
      - 10ml dye
      - 5g bleach
    Stage indicates which production stage uses this material
  '''
}

// ============================================================================
// LAYER 6: MACHINES
// ============================================================================

Table machine_assignment {
  id bigint [pk, increment]
  machine_id bigint [not null, ref: > machine.id]
  production_stage_id bigint [ref: > production_stage.id]
  plan_stage_id bigint [ref: > production_plan_stage.id]
  reservation_type varchar(20) [not null, default: 'PRODUCTION']
  reservation_status varchar(20) [not null, default: 'ACTIVE']
  assigned_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  released_at timestamp
  
  Note: '''
    Layer 6 - Equipment: Many-to-Many junction (machine ←→ stage)
    Reservation types: PRODUCTION, PLAN
    Reservation status: ACTIVE, RELEASED
    Tracks which machine is assigned to which production stage or plan stage
  '''
}

Table machine_maintenance {
  id bigint [pk, increment]
  machine_id bigint [not null, ref: > machine.id]
  maintenance_type varchar(20) [not null]
  issue_description text
  resolution text
  reported_by bigint [ref: > user.id]
  assigned_to bigint [ref: > user.id]
  reported_at timestamp [not null]
  started_at timestamp
  completed_at timestamp
  status varchar(20) [default: 'REPORTED']
  cost decimal(10,2)
  downtime_minutes int
  
  Note: '''
    Layer 6 - Equipment: Maintenance history and costs
    Maintenance types: SCHEDULED, BREAKDOWN, REPAIR, CALIBRATION
    Status values: REPORTED, IN_PROGRESS, COMPLETED, CANCELED
    Tracks downtime for Overall Equipment Effectiveness (OEE) calculation
  '''
}

// ============================================================================
// LAYER 7: INVENTORY
// ============================================================================

Table material_stock {
  id bigint [pk, increment]
  material_id bigint [not null, ref: > material.id]
  quantity decimal(10,3) [not null]
  unit varchar(20) [default: 'KG']
  unit_price decimal(12,2)
  location varchar(100)
  batch_number varchar(50)
  received_date date
  expiry_date date
  last_updated_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 7 - Inventory: Current material stock levels per location
    Real-time inventory tracking
    Supports batch tracking and expiry date management
    Location: Warehouse section (A1, A2, B1, etc.)
  '''
}

Table material_transaction {
  id bigint [pk, increment]
  material_id bigint [not null, ref: > material.id]
  transaction_type varchar(10) [not null]
  quantity decimal(10,3) [not null]
  unit varchar(20) [default: 'KG']
  reference_type varchar(50)
  reference_id bigint
  batch_number varchar(50)
  location varchar(100)
  notes text
  created_by bigint [not null, ref: > user.id]
  created_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 7 - Inventory: Material movements history (complete audit trail)
    Transaction types: IN, OUT, ADJUST
    Quantity: Positive for IN, negative for OUT
    Reference types: PURCHASE, REQUISITION, PRODUCTION_LOSS, ADJUSTMENT
    Every inventory change is logged here
  '''
}

Table finished_goods_stock {
  id bigint [pk, increment]
  product_id bigint [not null, ref: > product.id]
  quantity decimal(10,2) [not null]
  unit varchar(20) [default: 'UNIT']
  location varchar(100)
  batch_number varchar(50)
  production_date date
  quality_grade varchar(20) [default: 'A']
  qc_inspection_id bigint [ref: > qc_inspection.id]
  last_updated_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 7 - Inventory: Finished products inventory
    Quality grades: A (First-class), B (Second-class), C (Third-class), DEFECT
    Links to QC inspection for quality verification
    Location: FG-A1, FG-A2 (Finished Goods warehouse sections)
  '''
}

Table finished_goods_transaction {
  id bigint [pk, increment]
  product_id bigint [not null, ref: > product.id]
  transaction_type varchar(10) [not null]
  quantity decimal(10,2) [not null]
  unit varchar(20) [default: 'UNIT']
  reference_type varchar(50)
  reference_id bigint
  batch_number varchar(50)
  location varchar(100)
  quality_grade varchar(20)
  notes text
  created_by bigint [not null, ref: > user.id]
  created_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 7 - Inventory: Finished goods movements
    Transaction types: RECEIVE, SHIP, ADJUST, TRANSFER
    Reference types: PRODUCTION_COMPLETE, SALES_ORDER, ADJUSTMENT
    Complete audit trail for finished goods inventory
  '''
}

// ============================================================================
// LAYER 8: SYSTEM
// ============================================================================

Table notification {
  id bigint [pk, increment]
  user_id bigint [not null, ref: > user.id]
  type varchar(20) [not null]
  category varchar(20) [not null]
  title varchar(255) [not null]
  message text
  reference_type varchar(50)
  reference_id bigint
  is_read boolean [default: false]
  read_at timestamp
  created_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 8 - System: In-app notifications for users
    Types: INFO, WARNING, ERROR, SUCCESS
    Categories: ORDER, PRODUCTION, QC, MAINTENANCE, PAYMENT, SYSTEM
    Examples:
      - "Contract CON-20251013-001 requires your approval"
      - "Machine WEAVE-02 reported breakdown"
      - "QC inspection failed for WO-20251013-005"
  '''
}

Table report_template {
  id bigint [pk, increment]
  template_name varchar(255) [not null]
  report_type varchar(50) [not null]
  description text
  sql_query text
  parameters json
  chart_config json
  is_active boolean [default: true]
  created_by bigint [ref: > user.id]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp
  
  Note: '''
    Layer 8 - System: Reusable reports for dashboards
    Report types: PRODUCTION, QUALITY, INVENTORY, FINANCIAL
    Examples:
      - Production efficiency by machine
      - QC pass/fail trends
      - Material consumption by product
      - Payment status by customer
    Supports parameterized SQL queries and chart configurations
  '''
}

Table audit_log {
  id bigint [pk, increment]
  user_id bigint [ref: > user.id]
  action varchar(100) [not null]
  entity_type varchar(50) [not null]
  entity_id bigint
  old_value json
  new_value json
  ip_address varchar(50)
  user_agent text
  created_at timestamp [not null, default: `CURRENT_TIMESTAMP`]
  
  Note: '''
    Layer 8 - System: Complete audit trail
    Actions: CREATE, UPDATE, DELETE, LOGIN, APPROVE
    Entity types: contract, production_order, payment, etc.
    Stores before/after state in JSON format
    Security, compliance, debugging
    Immutable log - never delete
  '''
}

