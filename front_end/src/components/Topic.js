import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import {useSubscription} from '@apollo/client';
import React, {useContext, useState} from "react";
import ReactJson from "react-json-view";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

const GET_TOPIC = gql`

subscription KafkaEvents($topic: String!, $filter: String!, $offset: String!) {
  kafkaEvents(topic: $topic, filter: $filter, offset: $offset) {
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
            console.log("Data received from frontend " + data.subscriptionData.data.kafkaEvents.offset)
            if (!events.map(event => event.kafkaEvents.offset).includes(data.subscriptionData.data.kafkaEvents.offset)) {
                events.push(data.subscriptionData.data)
            }

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

    function handleOffset(event) {
        setOffset(event.target.value)
    }

    function shouldFilterViewItem(data) {
        return data.toString().includes(filterOnView)
    }

    function offsetSorter(a, b) {
        if (a.kafkaEvents.offset < b.kafkaEvents.offset){
            return -1
        } else{
            return 1
        }
    }

    const [events, setEvents] = useState([]);
    const [topic, setTopic] = useState("*")
    const [filter, setFilter] = useState("")
    const [filterOnView, setFilterOnView] = useState("")
    const [offset, setOffset] = useState("")
    const {loading, error, data} = useSubscription(GET_TOPIC, {
        variables: {topic: topic, filter: filter, offset: offset},
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
                <label>
                    From Offset &nbsp;
                    <input type="text" value={offset} onChange={handleOffset}/>
                </label>
                {/*<input type="submit" value="Submit" />*/}
            </form>
        </div>
        <div>
            <TableContainer component={Paper}>
                <Table sx={{minWidth: 100}} aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell align="right">Offset</TableCell>
                            <TableCell align="left">Data</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {events.filter(event => shouldFilterViewItem(event.kafkaEvents.data)).sort(offsetSorter).map(event =>
                            (
                                <TableRow
                                    key={event.kafkaEvents.offset}
                                    sx={{'&:last-child td, &:last-child th': {border: 0}}}
                                >
                                    <TableCell align="right">{event.kafkaEvents.offset}</TableCell>
                                    <TableCell align="left">
                                        <ReactJson
                                            src={JSON.parse(event.kafkaEvents.data)}
                                            name={false}
                                            theme="apathy:inverted"
                                            collapsed={true}
                                        />
                                    </TableCell>
                                </TableRow>

                            ))}
                    </TableBody>
                </Table>
            </TableContainer>
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
