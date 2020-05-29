# K8S namespace manager operator

### Description

Example of an K8S operator written in Java.
This operator can delete or scale down a namespace
when TTL elapses or when specified pods succeed.
It can also call a webhook.

### Demo

This demo has been tested on Linux in [minikube](https://github.com/kubernetes/minikube).

##### Preparation

* Install the operator:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl apply -f examples/operator.yml
```
* Verify it's running:
```
luke@tatooine:~/k8s-namespace-manager$ POD="$(kubectl get pod -n namespace-manager | grep namespace-manager-operator | cut -d' ' -f 1)"
luke@tatooine:~/k8s-namespace-manager$ kubectl logs ${POD} -n namespace-manager

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::  (v2.3.0.BUILD-SNAPSHOT)

2020-05-28 20:31:36.462  INFO 6 --- [           main] de.consol.labs.k8s.nsman.EntryPoint      : Starting EntryPoint v0.0.1-SNAPSHOT on namespace-manager-operator-cb585666b-t2lvp with PID 6 (/opt/consol/k8s-namespace-manager/app.jar started by root in /opt/consol/k8s-namespace-manager)
2020-05-28 20:31:36.465 DEBUG 6 --- [           main] de.consol.labs.k8s.nsman.EntryPoint      : Running with Spring Boot v2.3.0.BUILD-SNAPSHOT, Spring v5.2.6.RELEASE
2020-05-28 20:31:36.467  INFO 6 --- [           main] de.consol.labs.k8s.nsman.EntryPoint      : No active profile set, falling back to default profiles: default
2020-05-28 20:31:39.186  INFO 6 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService
2020-05-28 20:31:39.191  INFO 6 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService 'threadPoolTaskScheduler'
2020-05-28 20:31:39.197  INFO 6 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService
2020-05-28 20:31:40.121  INFO 6 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2020-05-28 20:31:40.765  INFO 6 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port(s): 8080
2020-05-28 20:31:40.782  INFO 6 --- [           main] de.consol.labs.k8s.nsman.EntryPoint      : Started EntryPoint in 5.138 seconds (JVM running for 6.271)
2020-05-28 20:31:40.794  INFO 6 --- [           main] d.c.labs.k8s.nsman.K8sNamespaceManager   : starting informers
2020-05-28 20:31:40.797  INFO 6 --- [           main] d.c.labs.k8s.nsman.K8sNamespaceManager   : informers have been started
2020-05-28 20:31:40.797  INFO 6 --- [           main] d.c.labs.k8s.nsman.K8sNamespaceManager   : waiting for informer to sync
2020-05-28 20:31:40.800  INFO 6 --- [amespaceManager] i.f.k.client.informers.cache.Controller  : informer#Controller: ready to run resync and reflector runnable
2020-05-28 20:31:40.805  INFO 6 --- [amespaceManager] i.f.k.client.informers.cache.Reflector   : Started ReflectorRunnable watch for class de.consol.labs.k8s.nsman.crd.NamespaceManager
2020-05-28 20:31:40.822  WARN 6 --- [amespaceManager] i.f.k.client.internal.VersionUsageUtils  : The client is using resource type 'namespacemanagers' with unstable version 'v1beta1'
2020-05-28 20:31:41.798  INFO 6 --- [           main] d.c.labs.k8s.nsman.K8sNamespaceManager   : informer has synced
2020-05-28 20:31:41.799  INFO 6 --- [        nsman-1] d.c.labs.k8s.nsman.task.TaskManager      : processing has been started
2020-05-28 20:31:41.799  INFO 6 --- [        nsman-1] d.c.labs.k8s.nsman.task.TaskManager      : queue is empty
```

##### Example 1

In this example opearator deletes a namespace after specified TTL is elapsed.

* Start example:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl apply -f examples/example-01.yml
```
* Verify `example-01` namespace has been created:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get ns
NAME                STATUS   AGE
default             Active   14d
example-01          Active   11s
kube-node-lease     Active   14d
kube-public         Active   14d
kube-system         Active   14d
namespace-manager   Active   10m
```
* Verify the TTL parameter:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get nsman example-01 -n example-01 -oyaml | grep -C 5 'ttlSeconds:'
  policies:
  - action:
      type: DELETE
    condition:
      params:
        ttlSeconds: 40
      type: TTL
    name: delete namespace when TTL elapses
```
* Observe how `example-01` namespace is going to deleted in ca. `40` seconds:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get ns -w
NAME                STATUS        AGE
default             Active        14d
example-01          Active        35s
kube-node-lease     Active        14d
kube-public         Active        14d
kube-system         Active        14d
namespace-manager   Active        10m
example-01          Terminating   42s
```

##### Example 2

In this example operator detects
that certain pods succeed
and it deletes a namespace then.

* Start example:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl apply -f examples/example-02.yml
```
* Verify `example-02` namespace has been created:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get ns
NAME                STATUS   AGE
default             Active   14d
example-02          Active   3s
kube-node-lease     Active   14d
kube-public         Active   14d
kube-system         Active   14d
namespace-manager   Active   31m
```
* Watch pods with labels `component=main,type=test` and wait for 3 completions:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get pod -l component=main,type=test -n example-02 -w
NAME                                    READY   STATUS              RESTARTS   AGE
my-consistent-tested-superb-app-769qj   1/1     Running             0          11s
my-consistent-tested-superb-app-pjqsr   0/1     ContainerCreating   0          11s
my-consistent-tested-superb-app-pjqsr   1/1     Running             0          15s
my-consistent-tested-superb-app-769qj   0/1     Completed           0          65s
my-consistent-tested-superb-app-pgfv8   0/1     Pending             0          0s
my-consistent-tested-superb-app-pgfv8   0/1     Pending             0          0s
my-consistent-tested-superb-app-pgfv8   0/1     ContainerCreating   0          0s
my-consistent-tested-superb-app-pgfv8   1/1     Running             0          5s
my-consistent-tested-superb-app-pjqsr   0/1     Completed           0          74s
my-consistent-tested-superb-app-pgfv8   0/1     Completed           0          65s
```
* Verify that `example-02` namespace has been deleted (or being terminated):
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get ns
NAME                STATUS   AGE
default             Active   14d
kube-node-lease     Active   14d
kube-public         Active   14d
kube-system         Active   14d
namespace-manager   Active   33m
```

##### Example 3

In this example operator detects
that certain pods succeed
and it scales down specified deployments
and stateful sets
in a namespace.

* Start example:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl apply -f examples/example-03.yml
```
* Watch pods in the `example-03` namespace.
observe 3 completions of the `my-lonely-chivalrous-careful-app-*` pods
and after that you should see a wave of pods terminations
because the operator has scaled down
deployment `my-marked-comfortable-curious-app`
and stateful sets `my-outrageous-seemly-unarmed-app`
and `my-imminent-whispering-wholesale-app`:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl get pod -w -n example-03
NAME                                                READY   STATUS              RESTARTS   AGE
my-hot-amusing-protective-app-567ccdfd65-vl4gp      0/1     ContainerCreating   0          11s
my-hot-amusing-protective-app-567ccdfd65-xhstx      0/1     ContainerCreating   0          11s
my-hot-amusing-protective-app-567ccdfd65-zzn7j      0/1     ContainerCreating   0          11s
my-imminent-whispering-wholesale-app-0              0/1     ContainerCreating   0          10s
my-lonely-chivalrous-careful-app-b4drz              0/1     ContainerCreating   0          11s
my-lonely-chivalrous-careful-app-l74r9              1/1     Running             0          11s
my-marked-comfortable-curious-app-689d947db-4klxb   1/1     Running             0          11s
my-marked-comfortable-curious-app-689d947db-j5nk5   0/1     ContainerCreating   0          11s
my-marked-comfortable-curious-app-689d947db-mphg6   0/1     ContainerCreating   0          11s
my-marked-comfortable-curious-app-689d947db-nrqqx   0/1     ContainerCreating   0          11s
my-marked-comfortable-curious-app-689d947db-vbjqh   0/1     ContainerCreating   0          11s
my-outrageous-seemly-unarmed-app-0                  0/1     ContainerCreating   0          11s
my-hot-amusing-protective-app-567ccdfd65-xhstx      1/1     Running             0          13s
my-lonely-chivalrous-careful-app-b4drz              1/1     Running             0          17s
my-hot-amusing-protective-app-567ccdfd65-vl4gp      1/1     Running             0          20s
my-hot-amusing-protective-app-567ccdfd65-zzn7j      1/1     Running             0          23s
my-imminent-whispering-wholesale-app-0              1/1     Running             0          25s
my-imminent-whispering-wholesale-app-1              0/1     Pending             0          0s
my-imminent-whispering-wholesale-app-1              0/1     Pending             0          0s
my-imminent-whispering-wholesale-app-1              0/1     ContainerCreating   0          0s
my-marked-comfortable-curious-app-689d947db-vbjqh   1/1     Running             0          29s
my-marked-comfortable-curious-app-689d947db-j5nk5   1/1     Running             0          32s
my-marked-comfortable-curious-app-689d947db-nrqqx   1/1     Running             0          35s
my-marked-comfortable-curious-app-689d947db-mphg6   1/1     Running             0          39s
my-outrageous-seemly-unarmed-app-0                  1/1     Running             0          42s
my-outrageous-seemly-unarmed-app-1                  0/1     Pending             0          0s
my-outrageous-seemly-unarmed-app-1                  0/1     Pending             0          0s
my-outrageous-seemly-unarmed-app-1                  0/1     ContainerCreating   0          1s
my-imminent-whispering-wholesale-app-1              1/1     Running             0          21s
my-imminent-whispering-wholesale-app-2              0/1     Pending             0          0s
my-imminent-whispering-wholesale-app-2              0/1     Pending             0          0s
my-imminent-whispering-wholesale-app-2              0/1     ContainerCreating   0          0s
my-outrageous-seemly-unarmed-app-1                  1/1     Running             0          8s
my-outrageous-seemly-unarmed-app-2                  0/1     Pending             0          0s
my-outrageous-seemly-unarmed-app-2                  0/1     Pending             0          0s
my-outrageous-seemly-unarmed-app-2                  0/1     ContainerCreating   0          0s
my-imminent-whispering-wholesale-app-2              1/1     Running             0          6s
my-outrageous-seemly-unarmed-app-2                  1/1     Running             0          6s
my-lonely-chivalrous-careful-app-l74r9              0/1     Completed           0          67s
my-lonely-chivalrous-careful-app-x7766              0/1     Pending             0          0s
my-lonely-chivalrous-careful-app-x7766              0/1     Pending             0          0s
my-lonely-chivalrous-careful-app-x7766              0/1     ContainerCreating   0          0s
my-lonely-chivalrous-careful-app-x7766              1/1     Running             0          5s
my-lonely-chivalrous-careful-app-b4drz              0/1     Completed           0          77s
my-lonely-chivalrous-careful-app-x7766              0/1     Completed           0          65s
my-marked-comfortable-curious-app-689d947db-j5nk5   1/1     Terminating         0          2m21s
my-marked-comfortable-curious-app-689d947db-vbjqh   1/1     Terminating         0          2m21s
my-marked-comfortable-curious-app-689d947db-4klxb   1/1     Terminating         0          2m21s
my-marked-comfortable-curious-app-689d947db-nrqqx   1/1     Terminating         0          2m21s
my-marked-comfortable-curious-app-689d947db-mphg6   1/1     Terminating         0          2m21s
my-imminent-whispering-wholesale-app-2              1/1     Terminating         0          94s
my-outrageous-seemly-unarmed-app-2                  1/1     Terminating         0          91s
my-marked-comfortable-curious-app-689d947db-j5nk5   0/1     Terminating         0          2m52s
my-marked-comfortable-curious-app-689d947db-4klxb   0/1     Terminating         0          2m53s
my-marked-comfortable-curious-app-689d947db-vbjqh   0/1     Terminating         0          2m53s
my-marked-comfortable-curious-app-689d947db-mphg6   0/1     Terminating         0          2m53s
my-imminent-whispering-wholesale-app-2              0/1     Terminating         0          2m6s
my-outrageous-seemly-unarmed-app-2                  0/1     Terminating         0          2m3s
my-marked-comfortable-curious-app-689d947db-nrqqx   0/1     Terminating         0          2m54s
my-marked-comfortable-curious-app-689d947db-j5nk5   0/1     Terminating         0          2m55s
my-marked-comfortable-curious-app-689d947db-j5nk5   0/1     Terminating         0          2m55s
my-marked-comfortable-curious-app-689d947db-vbjqh   0/1     Terminating         0          3m5s
my-marked-comfortable-curious-app-689d947db-vbjqh   0/1     Terminating         0          3m5s
my-marked-comfortable-curious-app-689d947db-nrqqx   0/1     Terminating         0          3m5s
my-marked-comfortable-curious-app-689d947db-nrqqx   0/1     Terminating         0          3m5s
my-outrageous-seemly-unarmed-app-2                  0/1     Terminating         0          2m15s
my-outrageous-seemly-unarmed-app-2                  0/1     Terminating         0          2m15s
my-outrageous-seemly-unarmed-app-1                  1/1     Terminating         0          2m23s
my-imminent-whispering-wholesale-app-2              0/1     Terminating         0          2m18s
my-imminent-whispering-wholesale-app-2              0/1     Terminating         0          2m18s
my-imminent-whispering-wholesale-app-1              1/1     Terminating         0          2m39s
my-marked-comfortable-curious-app-689d947db-mphg6   0/1     Terminating         0          3m5s
my-marked-comfortable-curious-app-689d947db-mphg6   0/1     Terminating         0          3m5s
my-marked-comfortable-curious-app-689d947db-4klxb   0/1     Terminating         0          3m6s
my-marked-comfortable-curious-app-689d947db-4klxb   0/1     Terminating         0          3m6s
my-imminent-whispering-wholesale-app-1              0/1     Terminating         0          3m10s
my-outrageous-seemly-unarmed-app-1                  0/1     Terminating         0          2m54s
my-imminent-whispering-wholesale-app-1              0/1     Terminating         0          3m11s
my-outrageous-seemly-unarmed-app-1                  0/1     Terminating         0          2m55s
```

##### Example 4

In this example operator detects
that certain pods succeed
and it calls per POST a specified webhook.

* Start example:
```
luke@tatooine:~/k8s-namespace-manager$ kubectl apply -f examples/example-04.yml
```
* Watch logs of the webhook pod.
The pod should record incoming POST request
from operator:
```
luke@tatooine:~/k8s-namespace-manager$ POD="$(kubectl get pod -n example-04 | grep webhooky | cut -d' ' -f 1)"
luke@tatooine:~/k8s-namespace-manager$ kubectl logs ${POD} -n example-04 -f
server is listening on port 80
received request at 2020-05-29T07:26:24.261Z
POST /api/webhook/my-super-awesome-webhook
headers:
{
  "accept-encoding": "gzip",
  "user-agent": "ReactorNetty/0.9.7.RELEASE",
  "host": "webhooky.example-04.svc.cluster.local",
  "accept": "*/*",
  "content-type": "application/json",
  "content-length": "457"
}
body:
{"namespaceManager":{"namespace":"example-04","name":"example-04"},"policy":{"name":"call webhook when specific pods succeed","condition":{"type":"PODS_SUCCEED","params":{"ttlSeconds":null,"podLabels":{"component":"main"},"initialDelaySeconds":60,"periodSeconds":10,"minChecks":null}},"action":{"type":"WEBHOOK","params":{"selectors":null,"url":"http://webhooky.example-04.svc.cluster.local/api/webhook/my-super-awesome-webhook"}}},"namespace":"example-04"}
response has been sent
```

### Cleanup

```
luke@tatooine:~/k8s-namespace-manager$ for def in $(ls examples/*.yml); do kubectl delete -f ${def} --ignore-not-found ; done
```

### Namespace manager

##### Core concepts

A namespace manager is a
[custom K8S resource](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources)
which is watched and processed by an operator.
The namespace manager has policies
which define how namespace is managed.
A policy consist of a condition and an action.
The condition describes an event that needs to happen
in order for the action to be applied to a managed namespace.
Currently there are 2 types of conditions:
* `TTL` -- condition evaluates to `true`
after specified amount of time elapses
starting from the creation of a namespace.
* `PODS_SUCCEED` -- condition evaluates to `true`
when specified pods reach the `Succeeded`
[phase](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-phase)
or when no pods are found
(in case K8S
deletes them
[automatically upon completion](https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/#clean-up-finished-jobs-automatically)).

If a condition evaluates to `true`,
meaning that the corresponding event has happened,
the operator applies the action to the managed namespace.
There are 3 possible actions:
* `DELETE` -- delete the namespace.
* `SCALE_DOWN` -- scale down specified deployments and stateful sets in the namespace.
* `WEBHOOK` -- call a specified webhook via HTTP POST.

Once an action is successfully applied,
the operators flips the `deactivated`
flag of the namespace manager to `true`
setting it effectively into a dormant state
i.e. the namespace manager
will not be processed until the `deactivated`
property is set back to `false` manually.
This is done on purpose because
a namespace cannot be deleted or scaled down
multiple times
(it just doesn't make sense)
and a webhook (usually) should
not be called multiple times either.

Multiple policies can be declared.
They all are evaluated and
the first one,
which evaluates to `true`,
wins
(because it basically turns
the namespace manager off).

By combining conditions
and actions one can
~~do epic shit~~
implement common
namespace management
strategies.
Suppose there is a namespace
dedicated to running E2E tests.
There are following common
namespace management strategies:
* Delete the namespace after
some fixed time e.g.
after 1 hour.
See
[example-01](examples/example-01.yml).
Such a policy can be used in addition
to other policies in order to
ensure that resources
will be reclaimed
in any case.
* Delete the namespace
when specific pods
running E2E tests
succeed.
See
[example-02](examples/example-02.yml).
* Scale down deployments
and stateful sets
in the namespace
when the pods
running E2E tests
succeed.
See
[example-03](examples/example-03.yml).
* Notify an external system
(via a webhook)
when the pods
running E2E tests
succeed.
See
[example-04](examples/example-04.yml).

The above mentioned strategies can be mixed
i.e. combined in a single namespace manager.
The `WEBHOOK` action also serves as
a sort of extension mechanism.
If some custom super special logic
needs to be executed upon
a completion of E2E tests,
it can be implemented
on a separate server
and be triggered
by a POST request
sent by the operator.

##### A word on scalability

<div style="text-align:center">
<img alt="a silly dad joke" src="https://pbs.twimg.com/media/CxzX0scXUAA21uo?format=jpg&name=200x400">
</div>

Currently, the operator is not designed
to run in cluster mode.
The operator's deployment can be scaled up
but no consensus mechanism
between operator instances are currently
implemented so the operator instances
will process the same namespace managers
concurrently which should not lead to
problems in most cases
because all the actions
are idempotent
(except for calling a webhook
because external non-idempotent logic
may be executed in this case).

Task distribution
(i.e. watching
and processing
the namespace managers)
usually demands
some kind of infrastructure
for synchronisation or reaching
a consensus
such as a shared database
or a
[Zookeeper](https://zookeeper.apache.org/).
Such an infrastructure would
naturally increase complexity
and
this operator is a **prototype**
rather than
a production-ready
highly available
resilient
fault-tolerant
self-healing
distributed
decentralized
AI-powered
blockchain-based
[CNCF-certified](https://www.cncf.io/certification/software-conformance)
enterprise-grade
`<paste your favorite buzzword in here>`
solution.
It doesn't mean that
it's useless though.
One operator instance
can easily handle
up to 30 namespace managers.
Maybe even more,
it has not been stress-tested.
