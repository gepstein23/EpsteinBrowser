# ECS task execution role (pull images, write logs)
resource "aws_iam_role" "ecs_execution" {
  name = "${var.prefix}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.prefix}-ecs-execution" }
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# API task role
resource "aws_iam_role" "api_task" {
  name = "${var.prefix}-api-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.prefix}-api-task" }
}

resource "aws_iam_role_policy" "api_task" {
  name = "${var.prefix}-api-task-policy"
  role = aws_iam_role.api_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:ListBucket"]
        Resource = [var.documents_bucket_arn, "${var.documents_bucket_arn}/*"]
      },
      {
        Effect   = "Allow"
        Action   = ["es:ESHttp*"]
        Resource = ["${var.opensearch_domain_arn}/*"]
      }
    ]
  })
}

# Ingestion task role
resource "aws_iam_role" "ingestion_task" {
  name = "${var.prefix}-ingestion-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.prefix}-ingestion-task" }
}

resource "aws_iam_role_policy" "ingestion_task" {
  name = "${var.prefix}-ingestion-task-policy"
  role = aws_iam_role.ingestion_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:ListBucket"]
        Resource = [var.documents_bucket_arn, "${var.documents_bucket_arn}/*"]
      },
      {
        Effect   = "Allow"
        Action   = ["sqs:SendMessage"]
        Resource = [var.extract_text_queue_arn]
      }
    ]
  })
}

# Worker task role
resource "aws_iam_role" "worker_task" {
  name = "${var.prefix}-worker-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.prefix}-worker-task" }
}

resource "aws_iam_role_policy" "worker_task" {
  name = "${var.prefix}-worker-task-policy"
  role = aws_iam_role.worker_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
        Resource = [var.documents_bucket_arn, "${var.documents_bucket_arn}/*"]
      },
      {
        Effect   = "Allow"
        Action   = ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"]
        Resource = [var.extract_text_queue_arn]
      },
      {
        Effect   = "Allow"
        Action   = ["es:ESHttp*"]
        Resource = ["${var.opensearch_domain_arn}/*"]
      }
    ]
  })
}

# Step Functions execution role
resource "aws_iam_role" "step_functions" {
  name = "${var.prefix}-step-functions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "states.amazonaws.com" }
    }]
  })

  tags = { Name = "${var.prefix}-step-functions" }
}

resource "aws_iam_role_policy" "step_functions" {
  name = "${var.prefix}-step-functions-policy"
  role = aws_iam_role.step_functions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask", "ecs:StopTask", "ecs:DescribeTasks"]
        Resource = ["*"]
      },
      {
        Effect   = "Allow"
        Action   = ["iam:PassRole"]
        Resource = [aws_iam_role.ecs_execution.arn, aws_iam_role.worker_task.arn]
      },
      {
        Effect   = "Allow"
        Action   = ["events:PutTargets", "events:PutRule", "events:DescribeRule"]
        Resource = ["*"]
      }
    ]
  })
}
