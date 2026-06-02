async function run() {
  const res = await fetch('https://search.maven.org/solrsearch/select?q=g:com.google.devtools.ksp+AND+a:symbol-processing-api&core=gav&rows=20&wt=json');
  const data = await res.json();
  console.log(data.response.docs.map(d => d.v));
}
run();
