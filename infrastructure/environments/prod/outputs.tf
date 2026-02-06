output "vpc_id" {
  value = module.vpc.vpc_id
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "aurora_endpoint" {
  value = module.aurora.cluster_endpoint
}

output "opensearch_endpoint" {
  value = module.opensearch.domain_endpoint
}

output "documents_bucket" {
  value = module.s3.bucket_name
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}
