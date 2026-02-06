module "vpc" {
  source             = "../../modules/vpc"
  environment        = var.environment
  prefix             = var.prefix
  availability_zones = var.availability_zones
}

module "s3" {
  source      = "../../modules/s3"
  environment = var.environment
  prefix      = var.prefix
}

module "ecr" {
  source      = "../../modules/ecr"
  environment = var.environment
  prefix      = var.prefix
}

module "sqs" {
  source      = "../../modules/sqs"
  environment = var.environment
  prefix      = var.prefix
}

module "monitoring" {
  source      = "../../modules/monitoring"
  environment = var.environment
  prefix      = var.prefix
  alert_email = var.alert_email
  dlq_name    = "${var.prefix}-extract-text-dlq"
}

module "iam" {
  source                 = "../../modules/iam"
  environment            = var.environment
  prefix                 = var.prefix
  documents_bucket_arn   = module.s3.bucket_arn
  opensearch_domain_arn  = module.opensearch.domain_arn
  extract_text_queue_arn = module.sqs.queue_arn
}

module "aurora" {
  source                     = "../../modules/aurora"
  environment                = var.environment
  prefix                     = var.prefix
  vpc_id                     = module.vpc.vpc_id
  subnet_ids                 = module.vpc.isolated_subnet_ids
  allowed_security_group_ids = [module.ecs.tasks_security_group_id]
  master_password            = var.aurora_master_password
}

module "opensearch" {
  source                     = "../../modules/opensearch"
  environment                = var.environment
  prefix                     = var.prefix
  vpc_id                     = module.vpc.vpc_id
  subnet_ids                 = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.ecs.tasks_security_group_id]
}

module "alb" {
  source            = "../../modules/alb"
  environment       = var.environment
  prefix            = var.prefix
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
}

module "ecs" {
  source                  = "../../modules/ecs"
  environment             = var.environment
  prefix                  = var.prefix
  aws_region              = var.aws_region
  vpc_id                  = module.vpc.vpc_id
  private_subnet_ids      = module.vpc.private_subnet_ids
  alb_security_group_id   = module.alb.security_group_id
  api_target_group_arn    = module.alb.api_target_group_arn
  execution_role_arn      = module.iam.ecs_execution_role_arn
  api_task_role_arn       = module.iam.api_task_role_arn
  ingestion_task_role_arn = module.iam.ingestion_task_role_arn
  worker_task_role_arn    = module.iam.worker_task_role_arn
  api_image               = module.ecr.repository_urls["api"]
  ingestion_image         = module.ecr.repository_urls["ingestion"]
  workers_image           = module.ecr.repository_urls["workers"]
  api_log_group           = module.monitoring.api_log_group_name
  ingestion_log_group     = module.monitoring.ingestion_log_group_name
  workers_log_group       = module.monitoring.workers_log_group_name
}

module "step_functions" {
  source                = "../../modules/step-functions"
  environment           = var.environment
  prefix                = var.prefix
  role_arn              = module.iam.step_functions_role_arn
  extract_text_task_arn = module.ecs.workers_task_definition_arn
  index_task_arn        = module.ecs.workers_task_definition_arn
}
