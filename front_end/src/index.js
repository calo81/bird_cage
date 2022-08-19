import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { split, HttpLink, ApolloClient, InMemoryCache, ApolloProvider } from '@apollo/client';
import { getMainDefinition } from '@apollo/client/utilities';

import { ApolloLink, Observable, } from '@apollo/client/core';
import { print } from 'graphql';
import { createClient } from 'graphql-sse';

class SSELink extends ApolloLink {
    constructor(options) {
        super();
        this.client = createClient(options);
    }
    request(operation) {
        return new Observable((sink) => {
            return this.client.subscribe(Object.assign(Object.assign({}, operation), { query: print(operation.query) }), {
                next: sink.next.bind(sink),
                complete: sink.complete.bind(sink),
                error: sink.error.bind(sink),
            });
        });
    }
}
const sseLink = new SSELink({
    url: 'http://localhost:8282/graphql',
    onMessage: console.log,
    credentials: 'include',
    headers: () => {
            return {"Content-Type": "application/json"};
    },
});

const httpLink = new HttpLink({
    uri: 'http://localhost:8282/graphql',
    credentials: 'include'
});

const splitLink = split(
    ({ query }) => {
        const definition = getMainDefinition(query);
        return (
            definition.kind === 'OperationDefinition' &&
            definition.operation === 'subscription'
        );
    },
    sseLink,
    httpLink
);

const client = new ApolloClient({
    link: splitLink,
    cache: new InMemoryCache(),
    credentials: 'include'
});

const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
  <ApolloProvider client={client}>
    <App />
  </ApolloProvider>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
