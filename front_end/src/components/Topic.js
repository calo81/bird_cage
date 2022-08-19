import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import {useSubscription} from '@apollo/client';
import React, {useContext, useState} from "react";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';

const GET_TOPIC = gql`

subscription KafkaEvents($topic: String!) {
  kafkaEvents(topic: $topic) {
    offset
  }
}

`;

export default function Topic(props) {

    function addData(data) {
        console.log(topic)
        if (data.subscriptionData.data.kafkaEvents.offset != "-1") {
            events.push(data.subscriptionData.data)
        }
        setEvents(events)

    }

    function changeTopic(event) {
        setTopic("fff")
    }

    const [events, setEvents] = useState([]);
    const [topic, setTopic] = useState("*")
    const {loading, error, data} = useSubscription(GET_TOPIC, {
        variables: {topic: topic},
        shouldResubscribe: false,
        skip: false,
        onSubscriptionData: addData
    });
    const {client} = useContext(getApolloContext())

    console.log({data, loading, error});
    if (error) {
        return <div>
            <h4>Error Fetching Data </h4>
            <div>
                <Link color="primary" href="#" onClick={changeTopic}>
                    Refresh
                </Link>
            </div>
        </div>;
    }
    return <div>
        <div>
            <h4>{events.map(event => (
                <div key={event.kafkaEvents.offset}>{event.kafkaEvents.offset}</div>
            ))}</h4>
        </div>
        <div>
            <div>
                <Link color="primary" href="#" onClick={changeTopic}>
                    Refresh
                </Link>
            </div>
        </div>
    </div>

}
