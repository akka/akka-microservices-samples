SHELL_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := ${SHELL_DIR}

antora_docker_image     := local/antora-doc
antora_docker_image_tag := latest

target_dir := ${ROOT_DIR}/target

all: build

local-preview: html-author-mode
	@echo "Access the documentation on http://localhost:8000"
	(cd ${target_dir}/; python3 -m http.server)

show:
	@echo ROOT_DIR: ${ROOT_DIR}
	@echo target_dir: ${target_dir}

clean:
	rm -rf ${target_dir}

docker-image:
	(cd ${ROOT_DIR}/antora-docker;  docker build -t ${antora_docker_image}:${antora_docker_image_tag} .)

build: clean html

html: clean docker-image
	docker run \
		-u $(shell id -u):$(shell id -g) \
		-v ${ROOT_DIR}:/antora \
		--rm \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		docs-source/site.yml
	@echo "Done ${target_dir}/index.html"
	@echo "# Tech Hub wants to know what to publish:"
	@echo "${target_dir}"

html-author-mode: clean docker-image
	mv docs-source/supplemental_ui/partials/head-scripts.hbs docs-source/supplemental_ui/partials/head-scripts.hbs.tmp
	docker run \
		-u $(shell id -u):$(shell id -g) \
		-v ${ROOT_DIR}:/antora \
		--rm \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		docs-source/author-mode-site.yml
	mv docs-source/supplemental_ui/partials/head-scripts.hbs.tmp docs-source/supplemental_ui/partials/head-scripts.hbs
	@echo "Done ${target_dir}/index.html"

check-links: docker-image
	docker run \
		-v ${ROOT_DIR}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		-c 'find /antora/docs-source -name '*.adoc' -print0 | xargs -0 -n1 asciidoc-link-check -p -c docs-source/asciidoc-link-check-config.json'

list-todos: html docker-image
	docker run \
		-v ${ROOT_DIR}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		-c 'find /antora/target/ -name "*.html" -print0 | xargs -0 grep -iE "TODO|FIXME|REVIEWERS|adoc"'
