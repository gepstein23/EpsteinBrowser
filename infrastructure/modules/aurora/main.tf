resource "aws_db_subnet_group" "main" {
  name       = "${var.prefix}-aurora"
  subnet_ids = var.subnet_ids

  tags = { Name = "${var.prefix}-aurora-subnet-group" }
}

resource "aws_security_group" "aurora" {
  name_prefix = "${var.prefix}-aurora-"
  vpc_id      = var.vpc_id
  description = "Aurora PostgreSQL security group"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = var.allowed_security_group_ids
    description     = "PostgreSQL from allowed services"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound"
  }

  tags = { Name = "${var.prefix}-aurora-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_rds_cluster" "main" {
  cluster_identifier = "${var.prefix}-aurora"
  engine             = "aurora-postgresql"
  engine_mode        = "provisioned"
  engine_version     = "16.4"
  database_name      = "epstein"
  master_username    = "epstein_admin"
  master_password    = var.master_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.aurora.id]

  storage_encrypted         = true
  skip_final_snapshot       = var.environment == "dev" ? true : false
  final_snapshot_identifier = var.environment == "dev" ? null : "${var.prefix}-aurora-final"

  serverlessv2_scaling_configuration {
    min_capacity = var.environment == "dev" ? 0 : 0.5
    max_capacity = var.environment == "dev" ? 2 : 8
  }

  tags = { Name = "${var.prefix}-aurora" }
}

resource "aws_rds_cluster_instance" "main" {
  count              = var.environment == "dev" ? 1 : 2
  identifier         = "${var.prefix}-aurora-${count.index}"
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.main.engine
  engine_version     = aws_rds_cluster.main.engine_version

  tags = { Name = "${var.prefix}-aurora-instance-${count.index}" }
}
