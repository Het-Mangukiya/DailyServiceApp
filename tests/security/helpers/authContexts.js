function asProviderA(testEnv) {
  return testEnv.authenticatedContext("providerA").firestore();
}

function asProviderB(testEnv) {
  return testEnv.authenticatedContext("providerB").firestore();
}

function asCustomerA(testEnv) {
  return testEnv.authenticatedContext("customerA").firestore();
}

function asCustomerB(testEnv) {
  return testEnv.authenticatedContext("customerB").firestore();
}

function asAnon(testEnv) {
  return testEnv.unauthenticatedContext().firestore();
}

module.exports = {
  asProviderA,
  asProviderB,
  asCustomerA,
  asCustomerB,
  asAnon
};
