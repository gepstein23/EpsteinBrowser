resource "aws_security_group" "opensearch" {
  name_prefix = "${var.prefix}-opensearch-"
  vpc_id      = var.vpc_id
  description = "OpenSearch security group"

  ingress {
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "HTTPS from allowed services"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound"
  }

  tags = { Name = "${var.prefix}-opensearch-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_opensearch_domain" "main" {
  domain_name    = var.prefix
  engine_version = "OpenSearch_2.17"

  cluster_config {
    instance_type  = var.environment == "dev" ? "t3.small.search" : "r6g.large.search"
    instance_count = var.environment == "dev" ? 1 : 2

    zone_awareness_enabled = var.environment != "dev"

    dynamic "zone_awareness_config" {
      for_each = var.environment != "dev" ? [1] : []
      content {
        availability_zone_count = 2
      }
    }
  }

  ebs_options {
    ebs_enabled = true
    volume_size = var.environment == "dev" ? 20 : 100
    volume_type = "gp3"
  }

  vpc_options {
    subnet_ids         = var.environment == "dev" ? [var.subnet_ids[0]] : var.subnet_ids
    security_group_ids = [aws_security_group.opensearch.id]
  }

  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-2019-07"
  }

  tags = { Name = "${var.prefix}-opensearch" }
}
