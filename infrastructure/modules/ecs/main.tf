resource "aws_ecs_cluster" "main" {
  name = "${var.prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = { Name = "${var.prefix}-cluster" }
}

resource "aws_security_group" "ecs_tasks" {
  name_prefix = "${var.prefix}-ecs-tasks-"
  vpc_id      = var.vpc_id
  description = "ECS Fargate tasks security group"

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.alb_security_group_id]
    description     = "HTTP from ALB"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound"
  }

  tags = { Name = "${var.prefix}-ecs-tasks-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_ecs_task_definition" "api" {
  family                   = "${var.prefix}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.environment == "dev" ? 256 : 512
  memory                   = var.environment == "dev" ? 512 : 1024
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.api_task_role_arn

  container_definitions = jsonencode([{
    name         = "api"
    image        = "${var.api_image}:latest"
    essential    = true
    portMappings = [{ containerPort = 8080, protocol = "tcp" }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = var.api_log_group
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "api"
      }
    }
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = var.environment }
    ]
  }])

  tags = { Name = "${var.prefix}-api-task" }
}

resource "aws_ecs_service" "api" {
  name            = "${var.prefix}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.environment == "dev" ? 1 : 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.api_target_group_arn
    container_name   = "api"
    container_port   = 8080
  }

  tags = { Name = "${var.prefix}-api-service" }
}

resource "aws_ecs_task_definition" "ingestion" {
  family                   = "${var.prefix}-ingestion"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.environment == "dev" ? 512 : 1024
  memory                   = var.environment == "dev" ? 1024 : 2048
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.ingestion_task_role_arn

  container_definitions = jsonencode([{
    name         = "ingestion"
    image        = "${var.ingestion_image}:latest"
    essential    = true
    portMappings = [{ containerPort = 8081, protocol = "tcp" }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = var.ingestion_log_group
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ingestion"
      }
    }
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = var.environment }
    ]
  }])

  tags = { Name = "${var.prefix}-ingestion-task" }
}

resource "aws_ecs_task_definition" "workers" {
  family                   = "${var.prefix}-workers"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.environment == "dev" ? 512 : 1024
  memory                   = var.environment == "dev" ? 1024 : 4096
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.worker_task_role_arn

  container_definitions = jsonencode([{
    name      = "workers"
    image     = "${var.workers_image}:latest"
    essential = true
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = var.workers_log_group
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "workers"
      }
    }
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = var.environment }
    ]
  }])

  tags = { Name = "${var.prefix}-workers-task" }
}
