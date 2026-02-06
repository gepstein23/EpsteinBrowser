resource "aws_sfn_state_machine" "document_pipeline" {
  name     = "${var.prefix}-document-pipeline"
  role_arn = var.role_arn

  definition = jsonencode({
    Comment = "Document processing pipeline"
    StartAt = "ExtractText"
    States = {
      ExtractText = {
        Type     = "Task"
        Resource = var.extract_text_task_arn
        Retry = [{
          ErrorEquals     = ["States.ALL"]
          IntervalSeconds = 30
          MaxAttempts     = 3
          BackoffRate     = 2
        }]
        Catch = [{
          ErrorEquals = ["States.ALL"]
          Next        = "ExtractFailed"
        }]
        Next = "IndexDocument"
      }
      IndexDocument = {
        Type     = "Task"
        Resource = var.index_task_arn
        Retry = [{
          ErrorEquals     = ["States.ALL"]
          IntervalSeconds = 10
          MaxAttempts     = 2
          BackoffRate     = 2
        }]
        Catch = [{
          ErrorEquals = ["States.ALL"]
          Next        = "IndexFailed"
        }]
        Next = "Success"
      }
      Success = {
        Type = "Succeed"
      }
      ExtractFailed = {
        Type  = "Fail"
        Error = "ExtractTextFailed"
        Cause = "Text extraction failed after retries"
      }
      IndexFailed = {
        Type  = "Fail"
        Error = "IndexDocumentFailed"
        Cause = "Document indexing failed after retries"
      }
    }
  })

  tags = { Name = "${var.prefix}-document-pipeline" }
}
