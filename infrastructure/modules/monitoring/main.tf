resource "aws_sns_topic" "alerts" {
  name = "${var.prefix}-alerts"
  tags = { Name = "${var.prefix}-alerts" }
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# Log groups for each service
resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${var.prefix}/api"
  retention_in_days = var.environment == "dev" ? 7 : 30
  tags              = { Name = "${var.prefix}-api-logs" }
}

resource "aws_cloudwatch_log_group" "ingestion" {
  name              = "/ecs/${var.prefix}/ingestion"
  retention_in_days = var.environment == "dev" ? 7 : 30
  tags              = { Name = "${var.prefix}-ingestion-logs" }
}

resource "aws_cloudwatch_log_group" "workers" {
  name              = "/ecs/${var.prefix}/workers"
  retention_in_days = var.environment == "dev" ? 7 : 30
  tags              = { Name = "${var.prefix}-workers-logs" }
}

# DLQ message alarm
resource "aws_cloudwatch_metric_alarm" "dlq_messages" {
  alarm_name          = "${var.prefix}-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "Messages in extract-text DLQ"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    QueueName = var.dlq_name
  }

  tags = { Name = "${var.prefix}-dlq-alarm" }
}
