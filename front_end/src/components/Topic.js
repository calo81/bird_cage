import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import {useSubscription} from '@apollo/client';
import React, {useContext, useState} from "react";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';

const GET_TOPIC = gql`

subscription {
  kafkaEvents {
    offset
  }
}

`;

export default function Topic(props) {

    function addData(data) {
        events.push(data.subscriptionData.data)
        setEvents(events)
        console.log(data)
    }

    async function refresh(event) {
        await client.refetchQueries({
            include: [GET_TOPIC],
        });
    }

    const [events, setEvents] = useState([]);

    const {loading, error, data} = useSubscription(GET_TOPIC, {
        variables: {},
        shouldResubscribe: false,
        skip: false,
        onSubscriptionData: addData
    });
    const {client} = useContext(getApolloContext())

    console.log({data, loading, error});
    if (error) {
        return <h4>Error</h4>;
    }
    return <h4>{events.map(event => (
        <div key={event.kafkaEvents.offset}>{event.kafkaEvents.offset}</div>
    ))}</h4>

}
