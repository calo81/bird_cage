import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import React, {useContext} from "react";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';

const GET_CLUSTER = gql`

query {
  cluster {
    id
    brokers {
      id
    }
  }
}

`;



export default function Cluster(props) {

    async function refresh(event) {
        await client.refetchQueries({
            include: [GET_CLUSTER],
        });
    }

    const {loading, error, data} = useQuery(GET_CLUSTER);
    const {client} = useContext(getApolloContext())

    if (loading) return null;
    if (error) return `Error! ${error}`;
    console.log(client)

    return (
        <div>
            <Title>Kafka Cluster</Title>
            <Typography component="p" variant="h4">
                ID: {data?.cluster.id}
            </Typography>
            <div>
                <Typography color="text.secondary" sx={{flex: 1}}>
                    Brokers:
                </Typography>
                {data?.cluster.brokers.map((broker) => (
                    <div key={broker.id}>
                        <li>{broker.id}</li>
                    </div>
                ))}
            </div>
            <div>
                <Link color="primary" href="#" onClick={refresh}>
                   Refresh
                </Link>
            </div>
        </div>

    );
}
