import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import {useSubscription} from '@apollo/client';
import React, {useContext, useState} from "react";
import ReactJson from "react-json-view";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';

const GET_TOPIC = gql`

subscription KafkaEvents($topic: String!, $filter: String!) {
  kafkaEvents(topic: $topic, filter: $filter) {
    offset
    data
  }
}

`;

export default function Topic(props) {

    function addData(data) {
        console.log(topic)
        console.log(filter)
        if (data.subscriptionData.data.kafkaEvents.offset != "-1") {
            events.push(data.subscriptionData.data)
        }
        setEvents(events)

    }

    function changeTopic(event) {
        setTopic("fff")
    }

    function handleChange(event) {
        setFilter(event.target.value)
    }

    function handleSubmit(event) {
        alert('A filter was submitted: ' + event.value());
        event.preventDefault();
    }

    function handleFilterOnViewChange(event) {
        setFilterOnView(event.target.value)
    }

    function shouldFilterViewItem(data) {
        return data.toString().includes(filterOnView)
    }

    const [events, setEvents] = useState([]);
    const [topic, setTopic] = useState("*")
    const [filter, setFilter] = useState("")
    const [filterOnView, setFilterOnView] = useState("")
    const {loading, error, data} = useSubscription(GET_TOPIC, {
        variables: {topic: topic, filter: filter},
        shouldResubscribe: true,
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
            <h2>Reading Topic "{topic}"</h2>
        </div>
        <div>
            <form onSubmit={handleSubmit}>
                <label>
                    Listen only Containing: &nbsp;
                    <input type="text" value={filter} onChange={handleChange}/>
                </label>
                <label>
                    Filter on View &nbsp;
                    <input type="text" value={filterOnView} onChange={handleFilterOnViewChange}/>
                </label>
                {/*<input type="submit" value="Submit" />*/}
            </form>
        </div>
        <div>
            <h4>{events.filter(event => shouldFilterViewItem(event.kafkaEvents.data)).map(event =>
                (

                    <div key={event.kafkaEvents.offset}>
                        <ReactJson
                            src={JSON.parse(event.kafkaEvents.data)}
                            name={false}
                            theme="apathy:inverted"
                            collapsed={true}
                        />
                    </div>
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
