package com.github.haenaryn.catalog.infrastructure.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface ProductSearchDocumentRepository extends ElasticsearchRepository<ProductSearchDocument, String> {
}
