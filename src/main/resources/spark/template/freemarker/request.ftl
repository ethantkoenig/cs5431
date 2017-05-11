<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="row">
        <div class="col-md-6">
            <form action="/requests" method="post">
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
                <input id="request-button" class="btn btn-primary" type="submit" data-url="/requests"
                       value="Request EzraCoinL">
            </form>
        </div>
        <div class="col-md-6">
            <#list requests as request>
                <div class="well">
                    <form action="/transact" method="post" class="transactform">
                        <h2>${request.touser} has requested $${request.amount}.</h2>
                        <p>${request.message}</p>
                        <div class="horizontal">
                            <div class="input-group col-md-8">
                                <input type="hidden" name="recipient" value="${request.touser}">
                                <input type="hidden" name="tranid" value="${request.tranid?c}">
                                <input type="hidden" name="amount" value="${request.amount?c}">
                                <input type="hidden" name="message" value="${request.message}">
                                <input type="password" id="transaction-password"
                                       class="form-control" placeholder="Password">
                                <span class="input-group-btn">
                                    <button class="btn btn-secondary btn-success" type="submit">Accept</button>
                                </span>
                            </div>
                            <div class="input-group">
                                <button class="btn btn-secondary btn-danger delete-request" data-tranid=${request.tranid?c}>
                                    Delete
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </#list>
            <div class="row" id="status-message"></div>
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
            $('#recipient-selector').append('<option value="" disabled>You have not authorized anyone to send you money.</option>');
        </script>
    </#if>

</div>
<!-- /.container -->
</@layout.master>

