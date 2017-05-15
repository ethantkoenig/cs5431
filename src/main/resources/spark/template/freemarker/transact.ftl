<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="col-sm-6 padding-top">
        <form action="/transact" method="post" class="transactform">
            <div class="form-group">
                <label>Recipient</label>
                <select class="form-control" name="recipient" id="recipient-selector">
                </select>
            </div>
            <div class="form-group">
                <label>Amount</label>
                <input type="number" class="form-control" name="amount">
            </div>
            <div class="form-group">
                <label>Message</label>
                <textarea id="message" maxlength="50" class="form-control" rows="4" name="message"></textarea>
            </div>
            <div class="form-group">
                <label>Password</label>
                <input type="password" class="form-control transaction-password">
            </div>
            <input id="transact-button" class="btn btn-primary" type="submit" value="Send EzraCoinL">
        </form>

        <div class="row" id="status-message"></div>
    </div>
    <div class="col-sm-6 padding-top">
        <div class="panel panel-default scroll">
            <div class="panel-heading">Transaction History</div>

            <table class="table">
                <tr>
                    <th>Type</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Message</th>
                    <th>Amount</th>
                </tr>
                <#list transactions as tran>
                    <tr>
                        <#if tran.request>
                            <td>Request</td>
                        <#else>
                            <td>Transaction</td>
                        </#if>
                        <td>${tran.fromUser}</td>
                        <td>${tran.toUser}</td>
                        <td>${tran.message}</td>
                        <td>$${tran.amount}</td>
                    </tr>
                </#list>
            </table>
        </div>
    </div>


    <#if friends?? && friends?has_content>
        <#list friends as friend>
            <script>
                $('#recipient-selector').append($('<option>', {value: "${friend}", text: "${friend}"}));
            </script>
        </#list>
    <#else>
        <script>
            $('#recipient-selector').append('<option value="" disabled>No one has authorized you to send them money.</option>');
        </script>
    </#if>
</div>

<!-- /.container -->
</@layout.master>

