import { Link } from "react-router-dom";

export default function Manual() {
  return (
    <div className="page">
      <div className="page-head">
        <h1>User manual</h1>
        <p className="page-sub">
          What this tool does, what the numbers mean, and how to read a result.
        </p>
      </div>

      <section className="card prose">
        <h2>What this is</h2>
        <p>
          Card fraud detection works by running rules over a stream of transactions and
          flagging the ones that look wrong. The hard part is not writing rules — it is
          knowing whether they work. In a real bank you find out weeks later, when
          customers dispute charges.
        </p>
        <p>
          This tool solves that by generating its own transactions with fraud
          deliberately planted inside them. Because it knows exactly which transactions
          are fraudulent, it can measure a rule's accuracy the instant it runs.
        </p>
      </section>

      <section className="card prose">
        <h2>The two numbers that matter</h2>
        <div className="def">
          <h3>Precision</h3>
          <p>
            Of everything the rules flagged, how much was actually fraud. Low precision
            means real customers are being declined at the till for no reason, and
            analysts are wasting time on alerts that go nowhere.
          </p>
        </div>
        <div className="def">
          <h3>Recall</h3>
          <p>
            Of all the fraud that existed, how much was caught. Low recall means money
            walking out the door.
          </p>
        </div>
        <p>
          <strong>They pull against each other.</strong> Loosen a rule and you catch more
          fraud but annoy more customers. Tighten it and the reverse. There is no
          setting that maximises both — choosing where to sit on that curve is a
          business decision about what a missed fraud costs versus what an investigation
          costs. The <Link to="/analysis">Analysis</Link> page makes that curve visible.
        </p>
      </section>

      <section className="card prose">
        <h2>The three rules</h2>
        <div className="def">
          <h3>Spend velocity</h3>
          <p>
            Flags an account that spends far more than its own average across a short
            window. Catches a stolen card being drained quickly. Note it compares each
            account against <em>itself</em> — someone who normally spends £15 and
            someone who normally spends £200 are judged by different standards.
          </p>
        </div>
        <div className="def">
          <h3>Amount outlier</h3>
          <p>
            Flags a single transaction far above or below the account's normal size.
            The lower bound matters as much as the upper: thieves test a stolen card
            with tiny purchases before using it properly.
          </p>
        </div>
        <div className="def">
          <h3>Geographic impossibility</h3>
          <p>
            Flags a card used in two places too far apart to travel between in the time
            elapsed. A purchase in Toronto and another in Vancouver 25 minutes later
            implies about 8,000 km/h, so one of them is not the cardholder.
          </p>
        </div>
      </section>

      <section className="card prose">
        <h2>The four attack patterns</h2>
        <table>
          <thead>
            <tr><th>Pattern</th><th>What it looks like</th><th>Why it is here</th></tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Burst</strong></td>
              <td>Six transactions 40 seconds apart, several times the usual amount</td>
              <td>A stolen card being drained</td>
            </tr>
            <tr>
              <td><strong>Card testing</strong></td>
              <td>Eight tiny transactions, 11 hours apart</td>
              <td>Checking a stolen card still works before selling it</td>
            </tr>
            <tr>
              <td><strong>Impossible travel</strong></td>
              <td>Two transactions 25 minutes apart in different cities</td>
              <td>The card is being used in two places at once</td>
            </tr>
            <tr>
              <td><strong>Shopping trip</strong> <span className="tag">legitimate</span></td>
              <td>Four transactions 45 seconds apart, ordinary amounts</td>
              <td>An honest customer who looks guilty — this is what makes precision hard</td>
            </tr>
          </tbody>
        </table>
        <p>
          That last one is deliberate. Without legitimate behaviour that resembles
          fraud, precision would be trivially perfect and the results would prove
          nothing.
        </p>
      </section>

      <section className="card prose">
        <h2>How to use it</h2>
        <ol className="steps">
          <li>
            <strong>Run a test with the defaults.</strong> Note the precision and recall.
          </li>
          <li>
            <strong>Change one setting</strong> and run again. Changing more than one at
            a time makes the result impossible to attribute.
          </li>
          <li>
            <strong>Compare in History.</strong> Every run is stored with its settings.
          </li>
          <li>
            <strong>Change the dataset seed</strong> and re-run your best configuration.
            A finding that disappears under a different seed was never real.
          </li>
        </ol>
      </section>

      <section className="card prose">
        <h2>Honest limitations</h2>
        <ul>
          <li>
            The data is synthetic. Real transaction traffic contains checkout retries,
            split payments and subscription batches that this does not model, so
            precision here is optimistic.
          </li>
          <li>
            Fraud is held near 1% of transactions, roughly in line with published card
            fraud rates. At higher rates precision would look artificially good.
          </li>
          <li>
            Three rules cannot catch everything. Some planted fraud is invisible to all
            of them by design — a tool that scored perfectly would be measuring its own
            assumptions.
          </li>
        </ul>
      </section>
    </div>
  );
}