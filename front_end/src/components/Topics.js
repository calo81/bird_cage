import {useQuery, useLazyQuery, gql} from '@apollo/client';
import {getApolloContext} from '@apollo/client';
import React, {useContext, useState} from "react";
// import * as React from 'react';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Title from './Title';

const GET_TOPICS= gql`

query {
  topics {
    name
  }
}

`;

export default function Topics(props) {

    async function refresh(event) {
        await client.refetchQueries({
            include: [GET_TOPICS],
        });
    }

    const {loading, error, data} = useQuery(GET_TOPICS);
    const {client} = useContext(getApolloContext())

    if (loading) return null;
    if (error) return `Error! ${error}`;
    console.log(client)

    return (
        <div>
            <Title>Topic</Title>
            <div>
                {data?.topics.map((topic) => (
                    <div key={topic.name}>
                        <li>{topic.name}</li>
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
