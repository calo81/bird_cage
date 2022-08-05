import { useQuery, useLazyQuery, gql } from '@apollo/client';

const GET_CLUSTER = gql`

  query GetCluster {
    cluster {
      id
      properties
      brokers
    }
  }
`;

export default function Cluster(props) {

    const [fetch, { loading, error, data }] = useLazyQuery(GET_CLUSTER);

    if (loading) return null;
    if (error) return `Error! ${error}`;

    if (props.refresh){
        fetch()
    }
    if (!props.visible) {
        return <div/>
    }
    return (

        <div>
            {data?.cluster.brokers}
        </div>

    );
}
