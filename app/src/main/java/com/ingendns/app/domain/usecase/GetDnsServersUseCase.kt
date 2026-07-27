package com.ingendns.app.domain.usecase

import com.ingendns.app.domain.repository.DnsRepository

class GetDnsServersUseCase(
    private val repository: DnsRepository
) {

    suspend operator fun invoke() =
        repository.getDnsServers()

}