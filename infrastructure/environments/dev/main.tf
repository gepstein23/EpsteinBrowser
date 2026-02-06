# Dev environment — wires all modules together
# Module implementations will be added in M2 (tasks 2.1–2.11)

# module "vpc" {
#   source             = "../../modules/vpc"
#   environment        = var.environment
#   prefix             = var.prefix
#   availability_zones = var.availability_zones
# }

# module "s3" {
#   source      = "../../modules/s3"
#   environment = var.environment
#   prefix      = var.prefix
# }

# module "aurora" {
#   source      = "../../modules/aurora"
#   environment = var.environment
#   prefix      = var.prefix
#   vpc_id      = module.vpc.vpc_id
#   subnet_ids  = module.vpc.isolated_subnet_ids
# }

# module "opensearch" {
#   source      = "../../modules/opensearch"
#   environment = var.environment
#   prefix      = var.prefix
#   vpc_id      = module.vpc.vpc_id
#   subnet_ids  = module.vpc.private_subnet_ids
# }

# module "ecr" {
#   source      = "../../modules/ecr"
#   environment = var.environment
#   prefix      = var.prefix
# }

# module "ecs" {
#   source             = "../../modules/ecs"
#   environment        = var.environment
#   prefix             = var.prefix
#   vpc_id             = module.vpc.vpc_id
#   private_subnet_ids = module.vpc.private_subnet_ids
# }

# module "alb" {
#   source            = "../../modules/alb"
#   environment       = var.environment
#   prefix            = var.prefix
#   vpc_id            = module.vpc.vpc_id
#   public_subnet_ids = module.vpc.public_subnet_ids
# }

# module "step_functions" {
#   source      = "../../modules/step-functions"
#   environment = var.environment
#   prefix      = var.prefix
# }

# module "sqs" {
#   source      = "../../modules/sqs"
#   environment = var.environment
#   prefix      = var.prefix
# }

# module "monitoring" {
#   source      = "../../modules/monitoring"
#   environment = var.environment
#   prefix      = var.prefix
# }

# module "iam" {
#   source      = "../../modules/iam"
#   environment = var.environment
#   prefix      = var.prefix
# }
