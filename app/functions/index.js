const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { struct } = require('pb-util');
const dialogflow = require('dialogflow');

admin.initializeApp({ credential: admin.credential.applicationDefault() });

const projectId = "schedly-266ff";
const languageCode = 'en-US';

exports.detectTextIntent = functions.https.onCall(async (data, context) => {
  // [START dialogflow_detect_intent_text]
  query = data.text;
  sessionID = data.sessionID;
  if (!query || !query.length) {
    return
  }
  // Instantiates a session client
  const sessionClient = new dialogflow.SessionsClient();

  // The path to identify the agent that owns the created intent.
  const sessionPath = sessionClient.sessionPath(projectId, sessionId);

  // Detects the intent of the queries.
  // The text query request.
  const request = {
    session: sessionPath,
    queryInput: {
      text: {
        text: query,
        languageCode: languageCode,
      },
    },
  };
  console.log(`Sending query "${query}"`);
  try {
    const responses = await sessionClient.detectIntent(request);
    console.log('Detected intent!!');
    logQueryResult(sessionClient, responses[0].queryResult);
    const result = responses[0].queryResult;
    const parameters = JSON.stringify(struct.decode(result.parameters));
    return {
      response: result.fulfillmentText,
      parameters: parameters
    };
  }
  catch (err) {
    console.error('ERROR:', err);
  }

  // [END dialogflow_detect_intent_text]
});

function logQueryResult(sessionClient, result) {
  console.log(`  Query: ${result.queryText}`);
  console.log(`  Response: ${result.fulfillmentText}`);
  if (result.intent) {
    console.log(`  Intent: ${result.intent.displayName}`);
  } else {
    console.log(`  No intent matched.`);
  }
  const parameters = JSON.stringify(struct.decode(result.parameters));
  console.log(`  Parameters: ${parameters}`);
}
