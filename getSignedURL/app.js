'use strict'

const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3')
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner')

const s3Client = new S3Client({ region: process.env.AWS_REGION })

const URL_EXPIRATION_SECONDS = 300

exports.handler = async (event) => {
  console.log('Received event:', JSON.stringify(event, null, 2));
  return await getUploadURL(event);
};

const getUploadURL = async function(event) {
  const { fileName, contentType } = event.queryStringParameters || {};

  if (!fileName || !contentType) {
    console.error('Missing required query parameters:', event.queryStringParameters);
    throw new Error('Missing required query parameters: fileName and contentType');
  }

  const s3Params = {
    Bucket: process.env.UploadBucket,
    Key: fileName,
    ContentType: contentType,
  };

  console.log('Params: ', s3Params);

  try {
    const command = new PutObjectCommand(s3Params);
    const uploadURL = await getSignedUrl(s3Client, command, { expiresIn: URL_EXPIRATION_SECONDS });
    return {
      statusCode: 200,
      body: JSON.stringify({
        uploadURL,
        Key: fileName
      })
    };
  } catch (error) {
    console.error('Error generating signed URL:', error);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: 'Failed to generate upload URL' })
    };
  }
};
