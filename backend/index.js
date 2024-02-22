const fastifyMultipart = require('@fastify/multipart')
const fs = require('node:fs')
const util = require('node:util')
const { pipeline } = require('node:stream')
const pump = util.promisify(pipeline)

// ================= Config =================
const upload_dir = 'uploads'
const base_url = 'https://iaup.liuzhen932.top/download'

const fastify = require('fastify')({
    logger: {
        level: 'info',
        transport: {
            target: 'pino-pretty',
            options: {
                translateTime: 'HH:MM:ss Z',
                ignore: 'pid,hostname'
            }
        }
    }
})
fastify.register(fastifyMultipart,
    {
        limits: {
            fieldNameSize: 640, // Max field name size in bytes
            fieldSize: 640,     // Max field value size in bytes
            fields: 5,         // Max number of non-file fields
            fileSize: 419430400,  // For multipart forms, the max file size in bytes == 50 MiB
            files: 1,           // Max number of file fields
            headerPairs: 10   // Max number of header
        }
    })

fastify.get('/', function (_request, reply) {
    reply.send({ code: 200 })
})

fastify.get('/ping', function (request, reply) {
    const ip = request.headers['cf-connecting-ip'] || request.ip
    reply.send({ code: 200, ip: request.ip, header: request.headers, ip: ip })
})

// ================= Core Code ================= 
fastify.post('/upload', async (request, reply) => {
    const ip = request.headers['cf-connecting-ip'] || request.ip
    try {
        const data = await request.file()
        if (data) {
            const filename = data.filename
            fastify.log.warn(`Someone from ${ip} is uploading ${filename}`)
            await pump(data.file, fs.createWriteStream(`${upload_dir}/${filename}`))
            fastify.log.warn(`Saved file ${filename}(from ${ip})!`)
            return reply.status(200).send({ status: 'success', url: `${base_url}/${filename}`, code: 200, msg: `Successfully upload file: ${filename}.` })
        } else {
            fastify.log.warn(`Someone is trying the POST upload interface, but he failed!`)
            return reply.status(400).send({ status: 'failure', code: 400, msg: `Failed to upload file(400): unknown filename` })
        }
    } catch (error) {
        return reply.status(500).send({ status: 'failure', code: 500, msg: `Failed to upload file(500): ${error}` })
    }
});

fastify.get('/upload', function (_request, reply) {
    reply.status(405).send({ code: 405, msg: 'Method Not Allowed' })
})

fastify.get('/download/:filename', function (request, reply) {
    const { filename } = request.params;
    const ip = request.headers['cf-connecting-ip'] || request.ip
    fs.readFile(`${upload_dir}/${filename}`, (err, data) => {
        if (err) {
            fastify.log.warn(`Someone from ${ip} want to download ${filename}, but he failed!`)
            return reply.status(500).send({ status: 'failure', code: 500, msg: `Failed to read file(500): ${err}` })
        }
        fastify.log.info(`Someone from ${ip} has downloaded ${filename}.`)
        return reply.status(200).header('Content-Type', 'application/zip').send(data);
    });
})

// Fire the server!
fastify.listen({ port: 3000 }, function (err) {
    if (err) {
        fastify.log.error(err)
        process.exit(1)
    }
})