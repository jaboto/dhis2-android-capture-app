package org.dhis2.usescases.jira

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.MediaType
import okhttp3.RequestBody
import org.dhis2.data.prefs.PreferenceProvider
import org.dhis2.utils.Constants
import org.dhis2.utils.jira.JiraIssueListRequest
import org.junit.Before
import org.junit.Test

class JiraViewModelTest {

    private val prefs: PreferenceProvider = mock()
    private val jiraViewModel: JiraViewModel = JiraViewModel()
    private val issueService: JiraViewModel.JiraIssueService = mock()

    @Before
    fun setup() {

    }

    @Test
    fun `Check jira issues`() {
        whenever(prefs.getString(Constants.JIRA_AUTH, null)) doReturn "a"
        whenever(prefs.contains(Constants.JIRA_USER)) doReturn true
        val basic = String.format("Basic %s", "a")
        val request = JiraIssueListRequest(prefs.getString(Constants.JIRA_USER, ""), 20)
        val requestBody =
                RequestBody.create(MediaType.parse("application/json"), Gson().toJson(request))
        whenever(issueService.getJiraIssues("", null)) doReturn
    }
}